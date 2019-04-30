import com.cloudbees.groovy.cps.NonCPS

@Grab('com.xlson.groovycsv:groovycsv:1.1')
import static com.xlson.groovycsv.CsvParser.parseCsv

def call() {
    handleStepErrors(stepName: 'checkHystrix') {
        String reportFileAsString = readFile("${s4SdkGlobals.reportsDirectory}/service_audits/aggregated_http_audit.log")

        final List<String> violations = extractViolations(reportFileAsString)

        if (!violations.isEmpty()) {
            currentBuild.result = 'FAILURE'
            error("Your project accesses downstream systems in a non-resilient manner:\n${violations.join("\n")}")
        }
    }
}

@NonCPS
List<String> extractViolations(String reportFileAsString) {
    List<String> columns = ['uri', 'threadName']
    def reportAsCsv = parseCsv([readFirstLine: true, columnNames: columns, quoteChar: '"', separator: ','], reportFileAsString)
    final List<String> violations = []
    for (line in reportAsCsv) {
        // Fixme: When using the vdm without hystrix in test code, the pipeline fails as a non resilient call is identified.
        // The thread in which the test code is executed is usually called main whereas threads inside a server started
        // by arquillian are named differently.
        // line.threadName == "main" is an indicator for the usage of the vdm in test code and thus should be allowed.
        if (!((line.threadName =~ /^hystrix-.+-\d+$/) || line.threadName == "main")) {
            violations.add("   - HTTP access to '${line.uri}' outside of hystrix context (thread was '${line.threadName}')")
        }
    }
    return violations
}
