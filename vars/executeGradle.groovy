import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:]) {

    handleStepErrors(stepName: 'executeGradle') {
        final script = parameters.script

        final Map stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, 'executeGradle')

        final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'executeGradle')

        Set parameterKeys = [
            'dockerImage',
            'localCache',
            'goals',
            'settingsFile'
        ]
        Set stepConfigurationKeys = [
            'dockerImage',
            'localCache',
            'settingsFile'
        ]

        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)

        String command = "gradle"

        def localCache = configuration.localCache
        if (localCache?.trim()) {
            command += " --project-cache-dir ${localCache}"
        }

        def settingsFile = configuration.settingsFile
        if (settingsFile?.trim()) {
            command += " --settings-file ${settingsFile}"
        }

        def goals = configuration.goals
        if (goals?.trim()) {
            command += " ${goals}"
        }

        dockerExecute(dockerImage: configuration.dockerImage) { sh command }
    }
}

