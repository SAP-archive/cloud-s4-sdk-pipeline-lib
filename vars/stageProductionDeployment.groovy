import com.sap.piper.ConfigurationLoader
import com.sap.cloud.sdk.s4hana.pipeline.EndToEndTestType

def call(Map parameters = [:]) {
    def stageName = 'productionDeployment'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {
        Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)

        lock(script.commonPipelineEnvironment.configuration.productionDeploymentLock) {
            if (fileExists('package.json')) {
                deployToCloudPlatform script: script, cfTargets: stageConfiguration.cfTargets, neoTargets: stageConfiguration.neoTargets, isProduction: true, stage: stageName, forceDowntimeDeployment: stageConfiguration.forceDowntimeDeployment
                executeEndToEndTest script: script, appUrls: stageConfiguration.appUrls, endToEndTestType: EndToEndTestType.SMOKE_TEST, stage: stageName
            } else {
                deployToCloudPlatform script: script, cfTargets: stageConfiguration.cfTargets, neoTargets: stageConfiguration.neoTargets, isProduction: true, stage: stageName, forceDowntimeDeployment: stageConfiguration.forceDowntimeDeployment
                echo "Smoke tests skipped, because package.json does not exist!"
            }
        }
    }
}
