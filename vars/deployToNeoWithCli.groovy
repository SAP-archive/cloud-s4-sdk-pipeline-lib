import com.sap.cloud.sdk.s4hana.pipeline.*

def call(Map parameters = [:]) {

    handleStepErrors(stepName: 'deployToNeoWithCli', stepParameters: parameters) {

        final script = parameters.script

        final Map stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, 'deployToNeoWithCli')

        final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'deployToNeoWithCli')

        List parameterKeys = ['dockerImage',
                              'deploymentType',
                              'target',
                              'source']
        List stepConfigurationKeys = ['dockerImage']

        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)

        ConfigurationHelper configurationHelper = new ConfigurationHelper(configuration)

        def dockerImage = configurationHelper.getMandatoryProperty('dockerImage')
        def deploymentDescriptors = configurationHelper.getMandatoryProperty('target')
        def source = configurationHelper.getMandatoryProperty('source')

        ConfigurationHelper deploymentDescriptor = new ConfigurationHelper(deploymentDescriptors)
        if (deploymentDescriptor.isPropertyDefined("credentialsId")) {
            NeoDeployCommandHelper commandHelper
            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: deploymentDescriptor.getConfigProperty('credentialsId'), passwordVariable: 'NEO_PASSWORD', usernameVariable: 'NEO_USERNAME']]) {
                commandHelper = new NeoDeployCommandHelper(deploymentDescriptors, NEO_USERNAME, BashUtils.escape(NEO_PASSWORD), source)
                deploy(dockerImage, configuration.deploymentType, commandHelper)
            }
        } else {
            throw new Exception("ERROR - SPECIFY credentialsId")
        }

    }
}

private deploy(dockerImage, deploymentType, NeoDeployCommandHelper commandHelper) {
    commandHelper.assertMandatoryParameters()
    executeDockerNative(dockerImage: dockerImage) {
        lock("deployToNeoWithCli:${commandHelper.resourceLock()}") {

            if (deploymentType == DeploymentType.ROLLING_UPDATE) {
                if (!isAppRunning(commandHelper)) {
                    deploymentType = DeploymentType.STANDARD
                    echo "Rolling update not possible because application is not running. Falling back to standard deployment."
                }
            }

            if (deploymentType == DeploymentType.ROLLING_UPDATE) {
                sh commandHelper.rollingUpdateCommand()
            } else {
                sh commandHelper.deployCommand()
                sh commandHelper.restartCommand()
            }
        }
    }
}

private boolean isAppRunning(NeoDeployCommandHelper commandHelper) {
    def status = sh script: "${commandHelper.statusCommand()} || true", returnStdout: true
    return status.contains('Status: STARTED')
}
