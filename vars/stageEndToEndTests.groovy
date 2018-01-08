import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationLoader
import com.sap.cloud.sdk.s4hana.pipeline.EndToEndTestType

def call(Map parameters = [:]) {
    def script = parameters.script
    runAsStage(stageName: 'endToEndTests', script: script) {
        final Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, 'endToEndTests')
        if (stageConfiguration) {
            lock(script.pipelineEnvironment.configuration.endToEndTestLock) {
                deployToCloudPlatform script: script, cfTargets: stageConfiguration.cfTargets, neoTargets: stageConfiguration.neoTargets, stage: 'endToEndTests'
                executeEndToEndTest script: script, appUrls: stageConfiguration.appUrls, endToEndTestType: EndToEndTestType.END_TO_END_TEST, stage: 'endToEndTests'
            }

        } else {
            echo "End to end tests skipped because no targets defined!"
        }
        echo "currentBuild.result: ${script.currentBuild.result}"
    }
}