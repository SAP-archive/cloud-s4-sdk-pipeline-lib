// keep because it is used by legacy consumers
def call(Map parameters) {

    Script script = parameters.script
    String name = 'Backend Unit Tests'
    String reportLocationPattern = 's4hana_pipeline/reports/backend-unit/**'

    npmExecuteScripts script: script, runScripts: ['ci-backend-unit-test']
    testsPublishResults script: script, junit: [active: true, pattern: reportLocationPattern, allowEmptyResults: true]
}
