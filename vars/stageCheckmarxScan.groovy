import com.sap.icd.jenkins.ConfigurationLoader
import com.sap.icd.jenkins.ConfigurationMerger

def call(Map parameters = [:]) {

    handleStepErrors(stepName: 'stageCheckmarxScan', stepParameters: parameters) {
        def script = parameters.script
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