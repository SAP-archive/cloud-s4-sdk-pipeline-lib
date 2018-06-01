import com.sap.cloud.sdk.s4hana.pipeline.BashUtils
import com.sap.cloud.sdk.s4hana.pipeline.DeploymentType
import com.sap.cloud.sdk.s4hana.pipeline.NeoDeployCommandHelper

import com.sap.piper.ConfigurationHelper
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:]) {

    handleStepErrors(stepName: 'deployToNeoWithCli', stepParameters: parameters) {

        final script = parameters.script

        final Map stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, 'deployToNeoWithCli')

        final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'deployToNeoWithCli')

        Set parameterKeys = [
            'dockerImage',
            'deploymentType',
            'target',
            'source'
        ]

        Set stepConfigurationKeys = ['dockerImage']

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

private deploy(dockerImage, DeploymentType deploymentType, NeoDeployCommandHelper commandHelper) {
    commandHelper.assertMandatoryParameters()
    dockerExecute(dockerImage: dockerImage) {
        lock("deployToNeoWithCli:${commandHelper.resourceLock()}") {

            if (deploymentType == DeploymentType.ROLLING_UPDATE) {
                if (!isAppRunning(commandHelper)) {
                    deploymentType = DeploymentType.STANDARD
                    echo "Rolling update not possible because application is not running. Falling back to standard deployment."
                }
            }

            echo "Link to the application dashboard: ${commandHelper.cloudCockpitLink()}"

            try {
                if (deploymentType == DeploymentType.ROLLING_UPDATE) {
                    sh commandHelper.rollingUpdateCommand()
                } else {
                    sh commandHelper.deployCommand()
                    sh commandHelper.restartCommand()
                }
            }
            catch (Exception ex) {
                echo "Error while deploying to SAP Cloud Platform. Here are the neo.sh logs:"
                sh "cat ${commandHelper.getNeoToolDirectory()}/log/*"
                throw ex
            }
        }
    }
}

private boolean isAppRunning(NeoDeployCommandHelper commandHelper) {
    def status = sh script: "${commandHelper.statusCommand()} || true", returnStdout: true
    return status.contains('Status: STARTED')
}
