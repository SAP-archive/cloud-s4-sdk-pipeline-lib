// Please note: always set min. threshold parameter and max. threshold parameter of the jacoco plugin together.
// Because when min. threshold is configured and the max. threshold is not, as default the max. = 0.
// Since min. is then greater than max., the min. is then set to 0 automatically.
// As a result, both max. and min. are set to 0, which is the default value.

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkCodeCoverage', stepParameters: parameters) {

        def script = parameters.script
        def jacocoExclusionPattern
        def jacocoExcludes = parameters.jacocoExcludes

        jacocoExclusionPattern = jacocoExcludes == null ? '' : jacocoExcludes.join(',')
        executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'Check Code Coverage', errorMessage: "Please examine Code Coverage results.") {

            //when coverage >= max., SUCCESSFUL
            //when max. > coverage >= min. UNSTABLE
            //when coverage < min. FAILURE
            jacoco execPattern: "${s4SdkGlobals.coverageReports}/*.exec", exclusionPattern: "${jacocoExclusionPattern}", changeBuildStatus: true, maximumLineCoverage: '70', minimumLineCoverage: '65'
        }
    }
}
