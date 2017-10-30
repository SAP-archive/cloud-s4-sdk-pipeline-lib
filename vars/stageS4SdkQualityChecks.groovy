import com.sap.icd.jenkins.ConfigurationLoader

def call(Map parameters = [:]) {
    handleStepErrors (stepName: 's4sdkQualityChecks', stepParameters: parameters) {
        def script = parameters.script
        unstashFiles script: script, stage: 'qualityChecks'

        Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, 's4SdkQualityChecks')

        checkDependencies script: script

        aggregateListenerLogs()

        checkCodeCoverage script: script, jacocoExcludes: stageConfiguration.jacocoExcludes
        checkHystrix()
        checkServices script: script, nonErpDestinations: stageConfiguration.nonErpDestinations

        stashFiles script: script, stage: 'qualityChecks'
    }
}
