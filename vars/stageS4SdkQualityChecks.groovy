import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 's4SdkQualityChecks'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {
        Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)

        checkDependencies script: script

        aggregateListenerLogs()

        checkCodeCoverage script: script, jacocoExcludes: stageConfiguration.jacocoExcludes
        checkHystrix()
        checkServices script: script, nonErpDestinations: stageConfiguration.nonErpDestinations
    }
}
