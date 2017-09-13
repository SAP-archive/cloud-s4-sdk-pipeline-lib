import com.sap.icd.jenkins.ConfigurationLoader

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'stagePerformanceTests', stepParameters: parameters) {
        def script = parameters.script
        unstashFiles script: script, stage: 'performanceTest'
        Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, 'performanceTest')
        if (stageConfiguration) {
            def deploymentType = parameters.get('deploymentType', 'standard')
            String project = env.JOB_NAME.tokenize('/').first()
            def lockName = "${project}/performanceTest"
            lock(lockName) {
                testDeployment script: script, stage: 'performanceTest', deploymentType: deploymentType, cfTargets: stageConfiguration.cfTargets ?: '', neoTargets: stageConfiguration.neoTargets ?: ''
                // Add JMeter performance tests
                if (ConfigurationLoader.stepConfiguration(script, 'checkJMeter')) {
                    def performanceTestReports = new File("${workspace}/${script.s4SdkGlobals.performanceReports}/JMeter")
                    performanceTestReports.mkdirs()
                    checkJMeter script: script, testPlan: "./performance-tests/*", reportDirectory: performanceTestReports
                }
            }

            echo "Current build result: ${script.currentBuild.currentResult}"
            errorWhenCurrentBuildResultIsWorseOrEqualTo(script.currentBuild, 'FAILURE', "Build was ABORTED and marked as FAILURE, please examine performance test results.")
        } else {
            echo "Performance tests have not been enabled. Skipping the stage."
        }
        stashFiles script: script, stage: 'performanceTest'
    }
}