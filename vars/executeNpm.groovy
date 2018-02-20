import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationLoader
import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationMerger

def call(Map parameters = [:], body) {

    handleStepErrors(stepName: 'executeNpm', stepParameters: parameters) {
        final script = parameters.script

        final Map stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, 'executeNpm')

        final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'executeNpm')

        List parameterKeys = [
            'dockerImage',
            'dockerOptions'
        ]
        List stepConfigurationKeys = ['dockerImage', 'defaultNpmRegistry']

        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)

        executeDockerNative(dockerImage: configuration.dockerImage, dockerOptions: configuration.dockerOptions) {
            try {
                if (configuration.defaultNpmRegistry) {
                    sh "npm config set registry ${configuration.defaultNpmRegistry}"
                }

                body()
            }
            catch(Exception e) {
                println "Error while executing npm. Here are the logs:"
                sh "cat ~/.npm/_logs/*"
                throw e;
            }
        }
    }
}


