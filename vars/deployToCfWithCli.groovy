import com.sap.cloud.sdk.s4hana.pipeline.BashUtils
import com.sap.cloud.sdk.s4hana.pipeline.CfTarget
import com.sap.cloud.sdk.s4hana.pipeline.DeploymentType
import com.sap.piper.ConfigurationMerger
import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {

    handleStepErrors(stepName: 'deployToCfWithCli', stepParameters: parameters) {

        final script = parameters.script

        final Map stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, 'deployToCfWithCli')

        final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'deployToCfWithCli')

        Set parameterKeys = [
            'dockerImage',
            'smokeTestStatusCode',
            'deploymentType',
            'org',
            'space',
            'apiEndpoint',
            'appName',
            'manifest',
            'credentialsId',
            'username',
            'password'
        ]
        Set stepConfigurationKeys = [
            'dockerImage',
            'smokeTestStatusCode',
            'org',
            'space',
            'apiEndpoint',
            'appName',
            'manifest',
            'credentialsId',
            'username',
            'password'
        ]

        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)
        CfTarget cfTarget = new CfTarget(configuration)
        cfTarget.validate()

        if (cfTarget.isCredentialsIdDefined()) {
            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: cfTarget.credentialsId, passwordVariable: 'CF_PASSWORD', usernameVariable: 'CF_USERNAME']]) {
                cfTarget.setUsername(CF_USERNAME)
                cfTarget.setPassword(BashUtils.escape(CF_PASSWORD))
                deploy(configuration.dockerImage, configuration.deploymentType, cfTarget, configuration.smokeTestStatusCode)
            }
        } else if (cfTarget.isUsernameAndPasswordDefined()) {
            deploy(configuration.dockerImage, configuration.deploymentType, cfTarget, configuration.smokeTestStatusCode)
        } else {
            throw new Exception("ERROR - EITHER SPECIFY credentialsId OR username and password")
        }
    }
}

private deploy(dockerImage, deploymentType, cfTarget, statusCode) {
    dockerExecute(dockerImage: dockerImage) {
        lock("${cfTarget.apiEndpoint}/${cfTarget.org}/${cfTarget.space}/${cfTarget.appName}") {
            if (deploymentType == DeploymentType.BLUE_GREEN) {
                withEnv(["STATUS_CODE=${statusCode}"]) {
                    def smokeTestScript = 'blue_green_check.sh'
                    writeFile file: smokeTestScript, text: libraryResource(smokeTestScript)
                    def smokeTest = '--smoke-test $(pwd)/' + smokeTestScript
                    sh "chmod +x ${smokeTestScript}"

                    sh "cf login -u ${cfTarget.username} -p ${cfTarget.password} -a ${cfTarget.apiEndpoint} -o ${cfTarget.org} -s ${cfTarget.space}"
                    sh "cf blue-green-deploy ${cfTarget.appName} -f ${cfTarget.manifest} ${smokeTest}"

                    sh "cf logout"
                }
            } else {
                sh "cf login -u ${cfTarget.username} -p ${cfTarget.password} -a ${cfTarget.apiEndpoint} -o ${cfTarget.org} -s ${cfTarget.space} && " + "cf push ${cfTarget.appName} -f ${cfTarget.manifest} && " + "cf logout"
            }
        }
    }
}
