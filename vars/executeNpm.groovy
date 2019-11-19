import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:], body) {

    handleStepErrors(stepName: 'executeNpm', stepParameters: parameters) {
        final script = parameters.script

        final Map stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, 'executeNpm')

        final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'executeNpm')

        Set parameterKeys = [
            'dockerImage',
            'dockerOptions',
            'defaultNpmRegistry',
            'sapNpmRegistry'
        ]
        Set stepConfigurationKeys = ['dockerImage', 'defaultNpmRegistry', 'sapNpmRegistry']

        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)

        dockerExecute(script: script, dockerImage: configuration.dockerImage, dockerOptions: configuration.dockerOptions) {
            try {
                if (configuration.defaultNpmRegistry) {
                    sh "npm config set registry ${configuration.defaultNpmRegistry}"
                }
                if (configuration.sapNpmRegistry) {
                    sh "npm config set @sap:registry ${configuration.sapNpmRegistry}"
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


