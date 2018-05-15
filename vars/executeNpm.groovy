import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:], body) {

    handleStepErrors(stepName: 'executeNpm', stepParameters: parameters) {
        final script = parameters.script

        final Map stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, 'executeNpm')

        final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'executeNpm')

        Set parameterKeys = [
            'dockerImage',
            'dockerOptions'
        ]
        Set stepConfigurationKeys = ['dockerImage', 'defaultNpmRegistry']

        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)

        dockerExecute(dockerImage: configuration.dockerImage, dockerOptions: configuration.dockerOptions) {
            try {
                if (configuration.defaultNpmRegistry) {
                    sh "npm config set registry ${configuration.defaultNpmRegistry}"
                }

                body()
            }
            catch (Exception e) {
                println "Error while executing npm. Here are the logs:"
                sh "cat ~/.npm/_logs/*"
                throw e
            }
        }
    }
}


