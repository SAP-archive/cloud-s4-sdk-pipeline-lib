import com.sap.icd.jenkins.ConfigurationLoader
import com.sap.icd.jenkins.EndToEndTestType

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'stageEndToEndTests', stepParameters: parameters) {
        final script = parameters.script

        final Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, 'endToEndTests')

        unstashFiles script: script, stage: 'endToEndTests'
        if (stageConfiguration) {
            lock(script.pipelineEnvironment.configuration.endToEndTestLock) {
                deployToCloudPlatform script: script, cfTargets: stageConfiguration.cfTargets, neoTargets: stageConfiguration.neoTargets
                executeEndToEndTest(script: script, appUrls: stageConfiguration.appUrls, endToEndTestType: EndToEndTestType.END_TO_END_TEST)
            }

        } else {
            echo "End to end tests skipped because no targets defined!"
        }
        stashFiles script: script, stage: 'endToEndTests'
        echo "currentBuild.result: ${script.currentBuild.result}"
    }
}