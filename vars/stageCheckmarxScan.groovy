import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:]) {
    def stageName = 'checkmarxScan'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {
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
            configuration.script = script
            try {
                dir('application') {
                    executeCheckmarxScan configuration
                }
            } finally {
                archiveArtifacts allowEmptyArchive: true, artifacts: '**/Checkmarx/Reports/ScanReport*'
            }
        }
    }
}
