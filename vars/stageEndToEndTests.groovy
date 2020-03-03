import com.sap.cloud.sdk.s4hana.pipeline.QualityCheck
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationLoader
import com.sap.cloud.sdk.s4hana.pipeline.EndToEndTestType

def call(Map parameters = [:]) {
    def stageName = 'endToEndTests'
    def script = parameters.script
    piperStageWrapper(stageName: stageName, script: script) {
        final Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)
        if (!stageConfiguration.cfTargets && !stageConfiguration.neoTargets) {
            error "End to end tests could not be executed as no deployment targets are defined. For more information, please visit https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/configuration.md#endtoendtests"
        }

        if(!stageConfiguration.appUrls) {
            error "End to end tests could not be executed as no appUrls are defined. For more information, please visit https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/configuration.md#endtoendtests"
        }

        lock(script.commonPipelineEnvironment.configuration.endToEndTestLock) {
            deployToCloudPlatform(
                script: script,
                cfTargets: stageConfiguration.cfTargets,
                neoTargets: stageConfiguration.neoTargets,
                cfCreateServices: stageConfiguration.cfCreateServices,
                enableZeroDowntimeDeployment: stageConfiguration.enableZeroDowntimeDeployment,
                stage: stageName
            )
            executeEndToEndTest script: script, appUrls: stageConfiguration.appUrls, endToEndTestType: EndToEndTestType.END_TO_END_TEST, stage: stageName
            ReportAggregator.instance.reportTestExecution(QualityCheck.EndToEndTests)
        }
    }
}
