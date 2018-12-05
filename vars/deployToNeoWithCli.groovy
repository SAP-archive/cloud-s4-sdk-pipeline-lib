import com.sap.cloud.sdk.s4hana.pipeline.BashUtils
import com.sap.cloud.sdk.s4hana.pipeline.DeploymentType
import com.sap.cloud.sdk.s4hana.pipeline.NeoDeployCommandHelper
import com.sap.piper.ConfigurationHelper

import groovy.transform.Field

@Field String STEP_NAME = 'deployToNeoWithCli'

@Field Set PARAMETER_KEYS = [
    'dockerImage',
    'deploymentType',
    'target',
    'source'
]

@Field Set STEP_CONFIG_KEYS = ['dockerImage']


def call(Map parameters = [:]) {

    handleStepErrors(stepName: STEP_NAME, stepParameters: parameters) {

        final script = parameters.script
        Map configuration = ConfigurationHelper.newInstance(this)
            .loadStepDefaults()
            .mixinStepConfig(script.commonPipelineEnvironment, STEP_CONFIG_KEYS)
            .mixin(parameters, PARAMETER_KEYS)
            .withMandatoryProperty('dockerImage')
            .withMandatoryProperty('target')
            .withMandatoryProperty('source')
            .use()

        def dockerImage = configuration.dockerImage
        def deploymentDescriptor = configuration.target
        def source = configuration.source

        verifyNeoEnvironmentVariables(deploymentDescriptor.environment)

        if (deploymentDescriptor.credentialsId) {
            NeoDeployCommandHelper commandHelper
            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: deploymentDescriptor.credentialsId, passwordVariable: 'NEO_PASSWORD', usernameVariable: 'NEO_USERNAME']]) {
                assertPasswordRules(NEO_PASSWORD)
                commandHelper = new NeoDeployCommandHelper(deploymentDescriptor, NEO_USERNAME, BashUtils.escape(NEO_PASSWORD), source)
                deploy(script, dockerImage, configuration.deploymentType, commandHelper)
            }
        } else {
            throw new Exception("ERROR - SPECIFY credentialsId")
        }

    }
}

private assertPasswordRules(String password){
    if(password.startsWith("@")){
        error("Your password for the deployment to SAP Cloud Platform contains characters which are not " +
            "supported by the neo tools. " +
            "For example it is not allowed that the password starts with @. " +
            "Please consult the documentation for the neo command line tool for more information: " +
            "https://help.sap.com/viewer/65de2977205c403bbc107264b8eccf4b/Cloud/en-US/8900b22376f84c609ee9baf5bf67130a.html")
    }
}

private deploy(script, dockerImage, DeploymentType deploymentType, NeoDeployCommandHelper commandHelper) {
    commandHelper.assertMandatoryParameters()
    dockerExecute(script: script, dockerImage: dockerImage) {
        lock("$STEP_NAME :${commandHelper.resourceLock()}") {

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

private void verifyNeoEnvironmentVariables(def targetEnvironmentVariables) {
    if (targetEnvironmentVariables && !(targetEnvironmentVariables in Map)) {
        throw new Exception("""The environment variables of the neoTargets configured in pipeline_config.yml are not correct defined.
Please use correct yaml description as documented here: https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/configuration.md#productiondeployment

Affected variables: ${targetEnvironmentVariables}""")
    }
}
