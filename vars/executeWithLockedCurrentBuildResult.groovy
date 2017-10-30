def call(Map parameters = [:], body) {
    handleStepErrors(stepName: 'executeWithLockedCurrentBuildResult', stepParameters: parameters) {
        def script = parameters.script
        def errorStatus = parameters.errorStatus
        def errorHandler = parameters.errorHandler
        def errorHandlerParameter = parameters.errorHandlerParameter
        def errorMessage = parameters.errorMessage

        lock(script.pipelineEnvironment.configuration.currentBuildResultLock){
            errorWhenCurrentBuildResultIsWorseOrEqualTo(script: script, errorStatus:'FAILURE', errorMessage: "Build was ABORTED and marked as FAILURE, because:\n currentBuild.result is ${script.currentBuild.result} \n and failure reason is ${buildFailureReason.FAILURE_REASON}.")
            body()
            errorWhenCurrentBuildResultIsWorseOrEqualTo(script: script, errorStatus: errorStatus, errorHandler: errorHandler, errorHandlerParameter: errorHandlerParameter, errorMessage: errorMessage)
        }
    }
}