import com.sap.cloud.sdk.s4hana.pipeline.QualityCheck
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator

def call(Map parameters = [:]) {
    def stageName = 'checkmarxScan'
    def script = parameters.script
    piperStageWrapper(stageName: stageName, script: script) {
        withEnv(["STAGE_NAME=$stageName"]) {
            checkmarxExecuteScan script: script, stageName: stageName
            ReportAggregator.instance.reportVulnerabilityScanExecution(QualityCheck.CheckmarxScan)
        }
    }
}
