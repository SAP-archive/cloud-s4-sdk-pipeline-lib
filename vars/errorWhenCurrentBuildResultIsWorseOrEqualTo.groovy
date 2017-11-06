def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'errorWhenCurrentBuildResultIsWorseOrEqualTo', stepParameters: parameters) {
        def script = parameters.script
        def errorStatus = parameters.errorStatus
        def errorHandler = parameters.errorHandler
        def errorHandlerParameter = parameters.errorHandlerParameter
        def errorMessage = parameters.errorMessage?:''

        if (script.currentBuild.result && script.currentBuild.resultIsWorseOrEqualTo(errorStatus)) {
            if (errorHandler) {
                errorHandler(errorHandlerParameter)
            }
            error(errorMessage)
        }
    }
}