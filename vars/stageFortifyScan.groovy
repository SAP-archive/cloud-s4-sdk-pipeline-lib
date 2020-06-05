import com.sap.cloud.sdk.s4hana.pipeline.QualityCheck
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator

def call(Map parameters = [:]) {
    def stageName = 'fortifyScan'
    def script = parameters.script
    piperStageWrapper(stageName: stageName, script: script) {
        fortifyExecuteScan script: script
        ReportAggregator.instance.reportVulnerabilityScanExecution(QualityCheck.FortifyScan)
    }
}
