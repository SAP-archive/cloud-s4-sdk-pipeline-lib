import com.sap.cloud.sdk.s4hana.pipeline.QualityCheck
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'endToEndTests'
    def script = parameters.script
    piperStageWrapper(stageName: stageName, script: script) {
        final Map stageConfiguration = loadEffectiveStageConfiguration(script: script, stageName: stageName)

        if (!stageConfiguration.cfTargets && !stageConfiguration.neoTargets) {
            error "End to end tests could not be executed as no deployment targets are defined. For more information, please visit https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/configuration.md#endtoendtests"
        }

        if(!stageConfiguration.appUrls) {
            error "End to end tests could not be executed as no appUrls are defined. For more information, please visit https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/configuration.md#endtoendtests"
        }

        lock(script.commonPipelineEnvironment.configuration.endToEndTestLock) {
            multicloudDeploy(
                script: script,
                stage: stageName
            )
            npmExecuteEndToEndTests script: script, runScript: 'ci-e2e', stageName: stageName
            ReportAggregator.instance.reportTestExecution(QualityCheck.EndToEndTests)
        }
    }
}
