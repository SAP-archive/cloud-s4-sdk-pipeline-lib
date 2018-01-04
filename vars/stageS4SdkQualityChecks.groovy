import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationLoader

def call(Map parameters = [:]) {
    def script = parameters.script
    runAsStage (stageName: 's4SdkQualityChecks', script: script) {
        unstashFiles script: script, stage: 'qualityChecks'

        Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, 's4SdkQualityChecks')

        checkDependencies script: script

        aggregateListenerLogs()

        checkCodeCoverage script: script, jacocoExcludes: stageConfiguration.jacocoExcludes
        checkHystrix()
        checkServices script: script, nonErpDestinations: stageConfiguration.nonErpDestinations

        stashFiles script: script, stage: 'qualityChecks'
        echo "currentBuild.result: ${script.currentBuild.result}"
    }
}
