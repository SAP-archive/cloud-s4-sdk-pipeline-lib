import com.sap.piper.ConfigurationLoader
import com.sap.cloud.sdk.s4hana.pipeline.EndToEndTestType

def call(Map parameters = [:]) {
    def stageName = 'endToEndTests'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {
        final Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)
        if (stageConfiguration) {
            lock(script.commonPipelineEnvironment.configuration.endToEndTestLock) {
                deployToCloudPlatform script: script, cfTargets: stageConfiguration.cfTargets, neoTargets: stageConfiguration.neoTargets, stage: stageName
                executeEndToEndTest script: script, appUrls: stageConfiguration.appUrls, endToEndTestType: EndToEndTestType.END_TO_END_TEST, stage: stageName
            }

        } else {
            echo "End to end tests skipped because no targets defined!"
        }
    }
}
