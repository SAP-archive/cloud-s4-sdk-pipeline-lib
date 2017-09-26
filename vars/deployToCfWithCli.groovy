import com.cloudbees.groovy.cps.NonCPS
import com.sap.icd.jenkins.ConfigurationHelper
import com.sap.icd.jenkins.ConfigurationLoader
import com.sap.icd.jenkins.ConfigurationMerger
import com.sap.icd.jenkins.ManifestUpdater

import java.util.regex.Matcher

def call(Map parameters = [:]) {

    handleStepErrors (stepName: 'deployToCfWithCli', stepParameters: parameters) {

        final script = parameters.script

        final Map stepDefaults = [
            dockerImage: 's4sdk/docker-cf-cli',
            smokeTestStatusCode: '200',
            'deploymentType': 'standard',
            'environmentVariables': ['destinations']]

        Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'deployToCfWithCli')

        List parameterKeys = [
            'dockerImage',
            'smokeTestStatusCode',
            'deploymentType',
            'targets',
            'environmentVariables'
        ]
        List stepConfigurationKeys = [
            'dockerImage',
            'smokeTestStatusCode',
            'deploymentType'
        ]

        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)

        def deploymentDescriptors = new ConfigurationHelper(configuration).getMandatoryProperty('targets')
        for (def i = 0; i<deploymentDescriptors.size(); i++) {
            ConfigurationHelper deploymentDescriptor = new ConfigurationHelper(deploymentDescriptors[i])
            def apiEndpoint = deploymentDescriptor.getMandatoryProperty('apiEndpoint')
            def org = deploymentDescriptor.getMandatoryProperty('org')
            def space = deploymentDescriptor.getMandatoryProperty('space')
            def appName = deploymentDescriptor.getMandatoryProperty('appName')
            def manifest = deploymentDescriptor.getMandatoryProperty('manifest')

            if (deploymentDescriptor.isPropertyDefined("credentialsId")) {
                withCredentials([
                    [$class: 'UsernamePasswordMultiBinding', credentialsId: deploymentDescriptor.getConfigProperty('credentialsId'), passwordVariable: 'CF_PASSWORD', usernameVariable: 'CF_USERNAME']
                ]) {
                    deploy(configuration.dockerImage, configuration.deploymentType, env.CF_USERNAME, env.CF_PASSWORD, apiEndpoint, org, space, appName, manifest, configuration.smokeTestStatusCode,  configuration.environmentVariables)
                }
            } else if (deploymentDescriptor.isPropertyDefined('username') && deploymentDescriptor.isPropertyDefined('password')) {
                deploy(configuration.dockerImage, configuration.deploymentType, deploymentDescriptor.getConfigProperty('username'), deploymentDescriptor.getConfigProperty('password'), apiEndpoint, org, space, appName, manifest, configuration.smokeTestStatusCode, configuration.environmentVariables)
            } else {
                throw new Exception("ERROR - EITHER SPECIFY credentialsId OR username and password")
            }
        }
    }
}

private deploy(dockerImage, deploymentType, username, password, apiEndpoint, org, space, appName, manifest, statusCode, environmentVariables){
    executeDockerNative(dockerImage: dockerImage) {
        lock("${apiEndpoint}/${org}/${space}/${appName}") {
            if (deploymentType == 'blue-green') {
                withEnv(["STATUS_CODE=${statusCode}"]) {
                    def smokeTestScript = 'blue_green_check.sh'
                    writeFile file: smokeTestScript, text: libraryResource(smokeTestScript)
                    def smokeTest = '--smoke-test $(pwd)/' + smokeTestScript
                    sh "chmod +x ${smokeTestScript}"

                    sh "cf login -u ${username} -p ${password} -a ${apiEndpoint} -o ${org} -s ${space}"
                    copyUserVariablesToManifest(appName, environmentVariables, manifest)
                    sh "cf blue-green-deploy ${appName} -f ${manifest} ${smokeTest}"

                    sh "cf logout"
                }
            } else {
                sh "cf login -u ${username} -p ${password} -a ${apiEndpoint} -o ${org} -s ${space} && " +
                        "cf push ${appName} -f ${manifest} && " +
                        "cf logout"
            }
        }
    }
}

private copyUserVariablesToManifest(appName, variablesToKeep, manifest){
    if(doesAppExists(appName)) {
        String environmentVariables = sh returnStdout: true, script: "cf env ${appName}"
        Map userVariables = extractUserVariables(environmentVariables, variablesToKeep)
        Map manifestMap = readYaml file: manifest
        ManifestUpdater updater = new ManifestUpdater(manifestMap)
        updater.addEnvironmentsVariables(userVariables)
        sh "rm ${manifest}"
        writeYaml file: manifest, data: updater.getManifest()
    }
}

@NonCPS
private extractUserVariables(environmentVariables, variablesToKeep){
    Map userVariables = [:]
    Matcher userEnvSectionMatcher = (environmentVariables =~ /User-Provided:\n(.+\n)*/)
    if (userEnvSectionMatcher.find()) {
        String userEnv = userEnvSectionMatcher.group()
        for(int i=0; i<variablesToKeep.size(); i++){
            String userEnvKey = variablesToKeep[i]
            Matcher userEnvMatcher = (userEnv =~ /${userEnvKey}:(.*)/)
            if (userEnvMatcher.find()) {
                String destinationEnvValue = userEnvMatcher.group(1)
                userVariables[userEnvKey] = destinationEnvValue
            }
        }
    }
    return userVariables
}

private doesAppExists(String appName){
    String apps = sh returnStdout:true, script:'cf apps'
    return apps.contains(appName)
}