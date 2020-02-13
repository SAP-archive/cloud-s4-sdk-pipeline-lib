
def call(Map parameters, Closure body) {
    Script script = parameters.script
    String name = 'Backend Unit Tests'
    String reportLocationPattern = '**/target/surefire-reports/*.xml'

    collectJUnitResults(script: script, testCategoryName: name, reportLocationPattern: reportLocationPattern) {
        body.call()
    }
}
