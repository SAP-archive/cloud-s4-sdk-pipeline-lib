// keep because it is used in extensions repo
def call(Map parameters) {

    Script script = parameters.script
    String name = 'Backend Unit Tests'
    String reportLocationPattern = 's4hana_pipeline/reports/backend-unit/**'

    runOverNpmModules(script: script, npmScripts: ['ci-backend-unit-test']) { basePath ->
        collectJUnitResults(script: script, testCategoryName: name, reportLocationPattern: reportLocationPattern) {
            executeNpm(script: script, dockerOptions: []) {
                dir(basePath) {
                    sh "npm run ci-backend-unit-test"
                }
            }
        }
    }
}
