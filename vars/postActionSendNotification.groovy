import hudson.model.Result
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

import com.cloudbees.groovy.cps.NonCPS


def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'postActionSendNotification', stepParameters: parameters) {
        def script = parameters.script

        Map postActionConfiguration = ConfigurationLoader.postActionConfiguration(script, 'sendNotification')
        Set postActionConfigurationKeys = ['recipients']
        Set parameterKeys = []
        Map defaults = [recipients: ['']]
        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, postActionConfiguration, postActionConfigurationKeys, defaults)

        Result currentBuildResult = Result.fromString(currentBuild.currentResult)
        Result previousBuildResult = latestBuildResult(currentBuild)

        if (isBackToSuccess(currentBuildResult, previousBuildResult) || isUnsuccessful(currentBuildResult)) {
            sendEmail(currentBuildResult, previousBuildResult, configuration.recipients)
        }
    }
}

def sendEmail(Result buildStatus, Result previousStatus, List recipients) {
    String subject = "Result of Job ${env.JOB_NAME} is ${buildStatus}"
    String body = "Build result of ${env.JOB_NAME} is ${buildStatus} (was ${previousStatus}). For more information, see: ${env.BUILD_URL}"
    String recipientsAsString = recipients.join(", ")

    emailext(recipientProviders: [[$class: 'CulpritsRecipientProvider']],
        to: recipientsAsString,
        subject: subject,
        body: body
    )
}

/**
 * @return Build result of latest finished build which was not aborted
 */
@NonCPS
Result latestBuildResult(def currentBuildRef) {
    def buildPointer = currentBuildRef.previousBuild
    while (buildPointer != null && isRunningOrAborted(stringAsResult(buildPointer.result))) {
        buildPointer = buildPointer.previousBuild
    }

    if (buildPointer == null) {
        // No earlier build result
        return null;
    } else {
        return Result.fromString(buildPointer.result)
    }
}

@NonCPS
boolean isUnsuccessful(Result result) {
    return result == Result.UNSTABLE || result == Result.FAILURE
}

@NonCPS
boolean isBackToSuccess(Result buildResult, Result previousBuildResult) {
    return buildResult == Result.SUCCESS && isUnsuccessful(previousBuildResult)
}

@NonCPS
boolean isRunningOrAborted(Result buildResult) {
    return buildResult == null || buildResult == Result.ABORTED
}

/**
 * Null-safe string to Result conversion
 */
@NonCPS
Result stringAsResult(String result) {
    return result ? Result.fromString(result) : null
}
