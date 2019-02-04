import com.sap.cloud.sdk.s4hana.pipeline.QualityCheck
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'performanceTests'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {

        final Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)

        if (stageConfiguration) {
            lock(script.commonPipelineEnvironment.configuration.performanceTestLock) {
                deployToCloudPlatform script: script, cfTargets: stageConfiguration.cfTargets, neoTargets: stageConfiguration.neoTargets, stage: stageName
                def jMeterConfig = ConfigurationLoader.stepConfiguration(script, 'checkJMeter')
                if (jMeterConfig && jMeterConfig.enabled != false) {
                    def performanceTestReports = new File("${workspace}/${script.s4SdkGlobals.performanceReports}/JMeter")
                    performanceTestReports.mkdirs()
                    checkJMeter script: script, reportDirectory: performanceTestReports
                    ReportAggregator.instance.reportPerformanceTestExecution(QualityCheck.JMeterTests)
                }

                if (ConfigurationLoader.stepConfiguration(script, 'checkGatling')?.enabled) {
                    checkGatling script: script, appUrls: stageConfiguration.appUrls
                    ReportAggregator.instance.reportPerformanceTestExecution(QualityCheck.GatlingTests)
                }
            }
        } else {
            echo "Performance tests have not been enabled. Skipping the stage."
        }
    }
}
