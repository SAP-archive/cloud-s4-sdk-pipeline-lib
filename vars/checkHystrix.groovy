@Grab('com.xlson.groovycsv:groovycsv:1.1')
import static com.xlson.groovycsv.CsvParser.parseCsv

def call() {
    handleStepErrors (stepName: 'checkHystrix') {

        String reportFileAsString = readFile("${s4SdkGlobals.reportsDirectory}/service_audits/aggregated_http_audit.log")
        List<String> columns = ['uri', 'threadName']
        def reportAsCsv = parseCsv([readFirstLine: true, columnNames: columns], reportFileAsString)

        final List<String> violations = []
        for (line in reportAsCsv) {
            echo "${line.uri} ${line.threadName}"
            if(!(line.threadName =~ /^hystrix-.+-\d+$/)) {
                violations.add("   - HTTP access to '${line.uri}' outside of hystrix context (thread was '${line.threadName}')")
            }
        }

        if(!violations.isEmpty()) {
            currentBuild.result = 'FAILURE'
            error("Your project accesses downstream systems in a non-resilient manner:\n${violations.join("\n")}")
        }
    }
}
