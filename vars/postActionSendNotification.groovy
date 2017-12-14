import com.sap.icd.jenkins.BuildResult

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'postActionSendNotification', stepParameters: parameters) {
        def script = parameters.script

        BuildResult buildResult = currentBuild.currentResult as BuildResult
        BuildResult previousBuildResult = currentBuild.previousBuild?.result as BuildResult

        if (isBuildFailure(buildResult)) {
            sendEmail (script: script, buildStatus: BuildResult.FAILURE)
        } else if (isBuildUnstable(buildResult)) {
            sendEmail (script: script, buildStatus: BuildResult.UNSTABLE)
        } else if (isBuildGreenAgain(buildResult, previousBuildResult)) {
            sendEmail (script: script, buildStatus: BuildResult.GREEN_AGAIN)
        }
    }
}

def isBuildFailure(buildResult) {
    return buildResult == BuildResult.FAILURE
}

def isBuildUnstable(buildResult) {
    return buildResult == BuildResult.UNSTABLE
}

def isBuildGreenAgain(buildResult, previousBuildResult) {
    return buildResult == BuildResult.SUCCESS && buildResult != previousBuildResult
}