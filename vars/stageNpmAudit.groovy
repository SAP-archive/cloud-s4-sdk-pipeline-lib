import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:]) {
    def stageName = 'npmAudit'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {
        final Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)
        final Map stageDefaults = ConfigurationLoader.defaultStageConfiguration(script, stageName)
        Set stageConfigurationKeys = [
            'auditedAdvisories'
        ]
        Map configuration = ConfigurationMerger.merge(stageConfiguration, stageConfigurationKeys, stageDefaults)

        checkNpmAudit(script: script, configuration: configuration, basePath: "./")
        if (script.commonPipelineEnvironment.configuration.isMta) {
            runOverModules(script: script, moduleType: "html5") { basePath ->
                checkNpmAudit(script: script, configuration: configuration, basePath: basePath)
            }
        }
    }
}
