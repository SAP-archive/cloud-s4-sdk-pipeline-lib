import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'postActionArchiveReport', stepParameters: parameters) {
        def script = parameters.script

        String result = ReportAggregator.instance.generateReport(script)

        if (parameters.printToConsole) {
            echo result
        }

        script.writeFile file: ReportAggregator.instance.fileName, text: result
        script.archiveArtifacts artifacts: ReportAggregator.instance.fileName
    }
}

