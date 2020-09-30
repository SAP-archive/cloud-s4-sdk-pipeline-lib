import com.sap.cloud.sdk.s4hana.pipeline.QualityCheck
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator

def call(Map parameters = [:]) {
    def stageName = 'whitesourceScan'
    def script = parameters.script
    piperStageWrapper(stageName: stageName, script: script) {
        whitesourceExecuteScan(script: script)
        ReportAggregator.instance.reportVulnerabilityScanExecution(QualityCheck.WhiteSourceScan)
    }
}
