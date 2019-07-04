import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

def call() {
    handleStepErrors(stepName: 'checkHystrix') {
        assertPluginIsActive('pipeline-utility-steps')
        String reportFile = "${s4SdkGlobals.reportsDirectory}/service_audits/aggregated_http_audit.log"
        final List<String> violations = extractViolations(reportFile)

        if (!violations.isEmpty()) {
            currentBuild.result = 'FAILURE'
            error("Your project accesses downstream systems in a non-resilient manner:\n${violations.join("\n")}")
        }
    }
}

List<String> extractViolations(String reportFile) {
    List reportAsCsvRecords = readCSV file: reportFile
    final List<String> violations = []
    for (int i = 0; i < reportAsCsvRecords.size(); i++) {
        // Fixme: When using the vdm without hystrix in test code, the pipeline fails as a non resilient call is identified.
        // The thread in which the test code is executed is usually called main whereas threads inside a server started
        // by arquillian are named differently.
        // line.threadName == "main" is an indicator for the usage of the vdm in test code and thus should be allowed.
        // [][] = [line][column]; report format: [uri, threadName]
        String threadName = reportAsCsvRecords[i][1].trim().replace('\"', '')
        String uri = reportAsCsvRecords[i][0].trim().replace('\"', '')
        if (!((threadName =~ /^hystrix-.+-\d+$/) || threadName == "main")) {
            violations.add("   - HTTP access to '$uri' outside of hystrix context (thread was '$threadName')")
        }
    }
    return violations
}
