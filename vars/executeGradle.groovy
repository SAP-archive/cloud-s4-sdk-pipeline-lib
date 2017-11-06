import com.sap.icd.jenkins.ConfigurationLoader
import com.sap.icd.jenkins.ConfigurationMerger

def call(Map parameters = [:]) {

    handleStepErrors(stepName: 'executeGradle') {
        final script = parameters.script

        final Map stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, 'executeGradle')

        final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'executeGradle')

        List parameterKeys = [
            'dockerImage',
            'localCache',
            'goals',
            'settingsFile'
        ]
        List stepConfigurationKeys = [
            'dockerImage',
            'localCache',
            'settingsFile'
        ]

        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)

        String command = "gradle"

        def localCache = configuration.localCache
        if(localCache?.trim()) {
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

        executeDockerNative(dockerImage: configuration.dockerImage) { sh command }
    }
}

