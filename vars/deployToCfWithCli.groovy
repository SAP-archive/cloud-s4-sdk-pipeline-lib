import com.sap.icd.jenkins.ConfigurationHelper
import com.sap.icd.jenkins.ConfigurationLoader
import com.sap.icd.jenkins.ConfigurationMerger

def call(Map parameters = [:]) {

    handleStepErrors (stepName: 'deployToCfWithCli', stepParameters: parameters) {

        final script = parameters.script

        final Map stepDefaults = [
            dockerImage: 's4sdk/docker-cf-cli',
            smokeTestStatusCode: '200',
            'deploymentType': 'standard'
        ]

        Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'deployToCfWithCli')

        List parameterKeys = [
            'dockerImage',
            'smokeTestStatusCode',
            'deploymentType',
            'targets'
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
                    deploy(configuration.dockerImage, configuration.deploymentType, env.CF_USERNAME, env.CF_PASSWORD, apiEndpoint, org, space, appName, manifest, configuration.smokeTestStatusCode)
                }
            } else if (deploymentDescriptor.isPropertyDefined('username') && deploymentDescriptor.isPropertyDefined('password')) {
                deploy(configuration.dockerImage, configuration.deploymentType, deploymentDescriptor.getConfigProperty('username'), deploymentDescriptor.getConfigProperty('password'), apiEndpoint, org, space, appName, manifest, configuration.smokeTestStatusCode)
            } else {
                throw new Exception("ERROR - EITHER SPECIFY credentialsId OR username and password")
            }
        }
    }
}

private deploy(dockerImage, deploymentType, username, password, apiEndpoint, org, space, appName, manifest, statusCode){
    executeDockerNative(dockerImage: dockerImage) {
        lock("${apiEndpoint}/${org}/${space}/${appName}") {
            if (deploymentType == 'blue-green') {
                withEnv(["STATUS_CODE=${statusCode}"]) {
                    def smokeTestScript = 'blue_green_check.sh'
                    writeFile file: smokeTestScript, text: libraryResource(smokeTestScript)
                    def smokeTest = '--smoke-test $(pwd)/' + smokeTestScript
                    sh "chmod +x ${smokeTestScript}"

                    sh "cf login -u ${username} -p ${password} -a ${apiEndpoint} -o ${org} -s ${space} && " +
                            "cf blue-green-deploy ${appName} -f ${manifest} ${smokeTest} && " +
                            "cf logout"
                }
            } else {
                sh "cf login -u ${username} -p ${password} -a ${apiEndpoint} -o ${org} -s ${space} && " +
                        "cf push ${appName} -f ${manifest} && " +
                        "cf logout"
            }
        }
    }
}