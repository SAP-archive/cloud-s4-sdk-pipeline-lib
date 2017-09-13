import com.sap.icd.jenkins.ConfigurationHelper
import com.sap.icd.jenkins.ConfigurationLoader
import com.sap.icd.jenkins.ConfigurationMerger
import com.sap.icd.jenkins.NeoDeployCommandHelper

def call(Map parameters = [:]) {

    handleStepErrors (stepName: 'deployToNeoWithCli', stepParameters: parameters) {

        final script = parameters.script

        final Map stepDefaults = [
            'deploymentType': 'standard'
        ]

        Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'deployToNeoWithCli')

        List parameterKeys = [
            'dockerImage',
            'deploymentType',
            'targets',
            'source'
        ]
        List stepConfigurationKeys = [
            'dockerImage',
            'deploymentType'
        ]

        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)

        ConfigurationHelper configurationHelper = new ConfigurationHelper(configuration)

        def dockerImage = configurationHelper.getMandatoryProperty('dockerImage')
        List deploymentDescriptors = configurationHelper.getMandatoryProperty('targets')
        def source = configurationHelper.getMandatoryProperty('source')

        for (def i = 0; i<deploymentDescriptors.size(); i++) {
            ConfigurationHelper deploymentDescriptor = new ConfigurationHelper(deploymentDescriptors[i])
            if (deploymentDescriptor.isPropertyDefined("credentialsId")) {
                NeoDeployCommandHelper commandHelper
                withCredentials([
                    [$class: 'UsernamePasswordMultiBinding', credentialsId: deploymentDescriptor.getConfigProperty('credentialsId'), passwordVariable: 'NEO_PASSWORD', usernameVariable: 'NEO_USERNAME']
                ]) {
                    commandHelper = new NeoDeployCommandHelper(deploymentDescriptors[i], env.NEO_USERNAME, env.NEO_PASSWORD, source)
                    deploy(dockerImage, configuration.deploymentType, commandHelper)
                }
            } else {
                throw new Exception("ERROR - SPECIFY credentialsId")
            }
        }
    }
}

private deploy(dockerImage, deploymentType, NeoDeployCommandHelper commandHelper){
    commandHelper.assertMandatoryParameters()
    executeDockerNative(dockerImage: dockerImage) {
        lock("deployToNeoWithCli:${commandHelper.resourceLock()}") {

            if (deploymentType == 'rolling-update') {
                if(!isAppRunning(commandHelper)){
                    deploymentType = 'standard'
                    echo "Rolling update not possible because application is not running. Falling back to standard deployment."
                }
            }

            if (deploymentType == 'rolling-update') {
                sh commandHelper.rollingUpdateCommand()
            } else {
                sh commandHelper.deployCommand()
                sh commandHelper.restartCommand()
            }
        }
    }
}

private boolean isAppRunning(NeoDeployCommandHelper commandHelper){
    def status = sh script: "${commandHelper.statusCommand()} || true", returnStdout: true
    return status.contains('Status: STARTED')
}
