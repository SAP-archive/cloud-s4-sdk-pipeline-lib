import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'performanceTests'
    def script = parameters.script
    piperStageWrapper(stageName: stageName, script: script) {

        final Map stageConfiguration = loadEffectiveStageConfiguration(script: script, stageName: stageName)

        if (stageConfiguration) {
            lock(script.commonPipelineEnvironment.configuration.performanceTestLock) {
                multicloudDeploy(
                    script: script,
                    stage: stageName
                )
                def jMeterConfig = ConfigurationLoader.stepConfiguration(script, 'checkJMeter')
                if (jMeterConfig && jMeterConfig.enabled != false) {
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
    }
}
