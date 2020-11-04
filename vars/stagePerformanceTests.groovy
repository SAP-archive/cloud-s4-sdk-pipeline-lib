import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = parameters.stageName ?: 'performanceTests'
    def script = parameters.script
    piperStageWrapper(stageName: stageName, script: script) {

        final Map stageConfiguration = loadEffectiveStageConfiguration(script: script, stageName: stageName)

        if (stageConfiguration) {
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

            def gatlingConfig = ConfigurationLoader.stepConfiguration(script, 'gatlingExecuteTests')
            if (gatlingConfig && gatlingConfig.enabled != false) {
                gatlingExecuteTests script: script
            }
        } else {
            echo "Performance tests have not been enabled. Skipping the stage."
        }
    }
}
