import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
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

        runOverNpmModules(script: script) { basePath ->
            checkNpmAudit(script: script, configuration: configuration, basePath: basePath)
        }

        ReportAggregator.instance.reportNpmSecurityScan(configuration.auditedAdvisories)
    }
}
