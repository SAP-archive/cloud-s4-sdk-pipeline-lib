import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationLoader
import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationMerger

def call(Map parameters = [:]) {

    def script = parameters.script
    runAsStage(stageName: 'checkmarxScan', script: script) {
        final Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, 'checkmarxScan')
        final Map stageDefaults = ConfigurationLoader.defaultStageConfiguration(script, 'checkmarxScan')

        List stageConfigurationKeys = ['groupId',
                                       'vulnerabilityThresholdMedium',
                                       'checkMarxProjectName',
                                       'vulnerabilityThresholdLow: 999999',
                                       'filterPattern',
                                       'fullScansScheduled',
                                       'generatePdfReport',
                                       'incremental',
                                       'preset',
                                       'checkmarxCredentialsId',
                                       'checkmarxServerUrl']

        Map configuration = ConfigurationMerger.merge(parameters, [], stageConfiguration, stageConfigurationKeys, stageDefaults)

        // only applicable if customized config exists
        if (stageConfiguration) {
            unstashFiles script: script, stage: 'checkmarxScan'
            configuration.script = script
            try {
                executeCheckmarxScan configuration
            } finally {
                archiveArtifacts allowEmptyArchive: true, artifacts: '**/Checkmarx/Reports/ScanReport*'
            }
            stashFiles script: script, stage: 'checkmarxScan'
        }
    }
}