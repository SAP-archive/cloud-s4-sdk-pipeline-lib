import com.sap.icd.jenkins.ConfigurationLoader

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'stagePerformanceTests', stepParameters: parameters) {
        def script = parameters.script
        unstashFiles script: script, stage: 'performanceTest'

        final Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, 'performanceTest')

        if (stageConfiguration) {
            lock(script.pipelineEnvironment.configuration.performanceTestLock) {
                deployToCloudPlatform script: script, cfTargets: stageConfiguration.cfTargets, neoTargets: stageConfiguration.neoTargets
                // Add JMeter performance tests
                if (ConfigurationLoader.stepConfiguration(script, 'checkJMeter')) {
                    def performanceTestReports = new File("${workspace}/${script.s4SdkGlobals.performanceReports}/JMeter")
                    performanceTestReports.mkdirs()
                    checkJMeter script: script, testPlan: "./performance-tests/*", reportDirectory: performanceTestReports
                }
            }
        } else {
            echo "Performance tests have not been enabled. Skipping the stage."
        }
        stashFiles script: script, stage: 'performanceTest'
    }
}
