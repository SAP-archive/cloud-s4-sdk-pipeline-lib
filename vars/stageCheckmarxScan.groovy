import com.sap.cloud.sdk.s4hana.pipeline.QualityCheck
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator

def call(Map parameters = [:]) {
    def stageName = 'checkmarxScan'
    def script = parameters.script
    piperStageWrapper(stageName: stageName, script: script) {
        checkmarxExecuteScan script: script
        ReportAggregator.instance.reportVulnerabilityScanExecution(QualityCheck.CheckmarxScan)
    }
}
