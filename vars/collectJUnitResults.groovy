def call(Map parameters, Closure body){
    Script script = parameters.script
    String testCategoryName = parameters.testCategoryName
    String reportLocationPattern = parameters.reportLocationPattern

    try {
        body()
    } catch (Exception e) {
        echo e.getLocalizedMessage()
        executeWithLockedCurrentBuildResult(
            script: script,
            errorStatus: 'FAILURE',
            errorHandler: script.buildFailureReason.setFailureReason,
            errorHandlerParameter: testCategoryName,
            errorMessage: "Please examine ${testCategoryName} report."
        ) {
            script.currentBuild.result = 'FAILURE'
        }
        throw e
    } finally {
        junit allowEmptyResults: true, testResults: reportLocationPattern
    }
}
