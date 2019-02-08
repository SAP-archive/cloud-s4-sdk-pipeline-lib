import com.sap.cloud.sdk.s4hana.pipeline.QualityCheck
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationLoader
import com.sap.cloud.sdk.s4hana.pipeline.EndToEndTestType

def call(Map parameters = [:]) {
    def stageName = 'endToEndTests'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {
        final Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)
        if (!stageConfiguration.cfTargets && !stageConfiguration.neoTargets) {
            error "End to end tests could not be executed as no deployment targets are defined."
        }

        if(!stageConfiguration.appUrls) {
            error "End to end tests could not be executed as no appUrls are defined."
        }

        lock(script.commonPipelineEnvironment.configuration.endToEndTestLock) {
            deployToCloudPlatform script: script, cfTargets: stageConfiguration.cfTargets, neoTargets: stageConfiguration.neoTargets, enableZeroDowntimeDeployment: stageConfiguration.enableZeroDowntimeDeployment, stage: stageName
            executeEndToEndTest script: script, appUrls: stageConfiguration.appUrls, endToEndTestType: EndToEndTestType.END_TO_END_TEST, stage: stageName
            ReportAggregator.instance.reportTestExecution(QualityCheck.EndToEndTests)
        }
    }
}
