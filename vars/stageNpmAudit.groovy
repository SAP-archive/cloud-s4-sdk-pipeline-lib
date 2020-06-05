import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:]) {
    def stageName = 'npmAudit'
    def script = parameters.script
    piperStageWrapper(stageName: stageName, script: script) {
        Set stageConfigurationKeys = [
            'auditedAdvisories'
        ]
        Map configuration = loadEffectiveStageConfiguration(script: script, stageName: stageName, stageConfigurationKeys: stageConfigurationKeys)

        runOverNpmModules(script: script) { basePath ->
            checkNpmAudit(script: script, configuration: configuration, basePath: basePath)
        }

        ReportAggregator.instance.reportNpmSecurityScan(configuration.auditedAdvisories)
    }
}
