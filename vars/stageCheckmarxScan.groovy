import com.sap.cloud.sdk.s4hana.pipeline.PathUtils
import com.sap.cloud.sdk.s4hana.pipeline.QualityCheck
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:]) {
    def stageName = 'checkmarxScan'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {
        runOverModules(script: script, moduleType: "java") { basePath ->
            executeCheckmarxScan(script, stageName, basePath)
        }

    }
}

private void executeCheckmarxScan( def script, String stageName, String basePath) {

    final Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)
    final Map stageDefaults = ConfigurationLoader.defaultStageConfiguration(script, stageName)

    Set stageConfigurationKeys = ['groupId',
                                  'vulnerabilityThresholdMedium',
                                  'checkMarxProjectName',
                                  'vulnerabilityThresholdLow',
                                  'filterPattern',
                                  'fullScansScheduled',
                                  'generatePdfReport',
                                  'incremental',
                                  'preset',
                                  'checkmarxCredentialsId',
                                  'checkmarxServerUrl']

    Map configuration = ConfigurationMerger.merge(stageConfiguration, stageConfigurationKeys, stageDefaults)

    // only applicable if customized config exists
    if (stageConfiguration) {
        String directory = PathUtils.normalize(basePath, 'application')
        configuration.script = script
        dir(directory) {
            executeCheckmarxScan configuration
        }

        ReportAggregator.instance.reportVulnerabilityScanExecution(QualityCheck.CheckmarxScan)
    }
}
