import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationLoader

def call(Map parameters = [:]) {
    def script = parameters.script
    runAsStage(stageName: 'performanceTest', script: script) {
        unstashFiles script: script, stage: 'performanceTest'

        final Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, 'performanceTest')

        if (stageConfiguration) {
            lock(script.pipelineEnvironment.configuration.performanceTestLock) {
                deployToCloudPlatform script: script, cfTargets: stageConfiguration.cfTargets, neoTargets: stageConfiguration.neoTargets, stage: 'performanceTest'
                def jMeterConfig = ConfigurationLoader.stepConfiguration(script, 'checkJMeter')
                if ( jMeterConfig && jMeterConfig.enabled!=false) {
                    def performanceTestReports = new File("${workspace}/${script.s4SdkGlobals.performanceReports}/JMeter")
                    performanceTestReports.mkdirs()
                    checkJMeter script: script, reportDirectory: performanceTestReports
                }

                if (ConfigurationLoader.stepConfiguration(script, 'checkGatling')?.enabled) {
                    checkGatling script: script, appUrls: stageConfiguration.appUrls
                }
            }
        } else {
            echo "Performance tests have not been enabled. Skipping the stage."
        }
        stashFiles script: script, stage: 'performanceTest'
        echo "currentBuild.result: ${script.currentBuild.result}"
    }
}
