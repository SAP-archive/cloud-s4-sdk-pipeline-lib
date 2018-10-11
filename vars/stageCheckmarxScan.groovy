import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:]) {
    def stageName = 'checkmarxScan'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {
        executeCheckmarxScan(script, stageName, "")
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
        String directory = basePath != null && !basePath.isEmpty() ? basePath : 'application'
        configuration.script = script
            dir(directory) {
                executeCheckmarxScan configuration
            }

    }
}
