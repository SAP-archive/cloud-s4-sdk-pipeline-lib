import com.cloudbees.groovy.cps.NonCPS
import com.sap.cloud.sdk.s4hana.pipeline.*

import java.util.regex.Matcher

def call(Map parameters = [:]) {

    handleStepErrors(stepName: 'deployToCfWithCli', stepParameters: parameters) {

        final script = parameters.script

        final Map stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, 'deployToCfWithCli')

        final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'deployToCfWithCli')

        List parameterKeys = ['dockerImage',
                              'smokeTestStatusCode',
                              'deploymentType',
                              'environmentVariables',
                              'org',
                              'space',
                              'apiEndpoint',
                              'appName',
                              'manifest',
                              'credentialsId',
                              'username',
                              'password']
        List stepConfigurationKeys = ['dockerImage',
                                      'smokeTestStatusCode',
                                      'org',
                                      'space',
                                      'apiEndpoint',
                                      'appName',
                                      'manifest',
                                      'credentialsId',
                                      'username',
                                      'password']

        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)
        CfTarget cfTarget = new CfTarget(configuration)
        cfTarget.validate()

        if (cfTarget.isCredentialsIdDefined()) {
            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: cfTarget.credentialsId, passwordVariable: 'CF_PASSWORD', usernameVariable: 'CF_USERNAME']]) {
                cfTarget.setUsername(CF_USERNAME)
                cfTarget.setPassword(BashUtils.escape(CF_PASSWORD))
                deploy(configuration.dockerImage, configuration.deploymentType, cfTarget, configuration.smokeTestStatusCode, configuration.environmentVariables)
            }
        } else if (cfTarget.isUsernameAndPasswordDefined()) {
            deploy(configuration.dockerImage, configuration.deploymentType, cfTarget, configuration.smokeTestStatusCode, configuration.environmentVariables)
        } else {
            throw new Exception("ERROR - EITHER SPECIFY credentialsId OR username and password")
        }
    }
}

private deploy(dockerImage, deploymentType, cfTarget, statusCode, environmentVariables) {
    executeDockerNative(dockerImage: dockerImage) {
        lock("${cfTarget.apiEndpoint}/${cfTarget.org}/${cfTarget.space}/${cfTarget.appName}") {
            if (deploymentType == DeploymentType.BLUE_GREEN) {
                withEnv(["STATUS_CODE=${statusCode}"]) {
                    def smokeTestScript = 'blue_green_check.sh'
                    writeFile file: smokeTestScript, text: libraryResource(smokeTestScript)
                    def smokeTest = '--smoke-test $(pwd)/' + smokeTestScript
                    sh "chmod +x ${smokeTestScript}"

                    sh "cf login -u ${cfTarget.username} -p ${cfTarget.password} -a ${cfTarget.apiEndpoint} -o ${cfTarget.org} -s ${cfTarget.space}"
                    copyUserVariablesToManifest(cfTarget.appName, environmentVariables, cfTarget.manifest)
                    sh "cf blue-green-deploy ${cfTarget.appName} -f ${cfTarget.manifest} ${smokeTest}"

                    sh "cf logout"
                }
            } else {
                sh "cf login -u ${cfTarget.username} -p ${cfTarget.password} -a ${cfTarget.apiEndpoint} -o ${cfTarget.org} -s ${cfTarget.space} && " + "cf push ${cfTarget.appName} -f ${cfTarget.manifest} && " + "cf logout"
            }
        }
    }
}

private copyUserVariablesToManifest(appName, variablesToKeep, manifest) {
    if (doesAppExists(appName)) {
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
private extractUserVariables(environmentVariables, variablesToKeep) {
    Map userVariables = [:]
    Matcher userEnvSectionMatcher = (environmentVariables =~ /User-Provided:\n(.+\n)*/)
    if (userEnvSectionMatcher.find()) {
        String userEnv = userEnvSectionMatcher.group()
        for (int i = 0; i < variablesToKeep.size(); i++) {
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

private doesAppExists(String appName) {
    String apps = sh returnStdout: true, script: 'cf apps'
    return apps.contains(appName)
}