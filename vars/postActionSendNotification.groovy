import hudson.model.Result
import com.sap.icd.jenkins.ConfigurationLoader
import com.sap.icd.jenkins.ConfigurationMerger


def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'postActionSendNotification', stepParameters: parameters) {
        def script = parameters.script

        Map postActionConfiguration = ConfigurationLoader.postActionConfiguration(script, 'sendNotification')
        List postActionConfigurationKeys = ['recipients']
        List parameterKeys = []
        Map defaults = [recipients: ['']]
        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, postActionConfiguration, postActionConfigurationKeys, defaults)

        Result buildResult = Result.fromString(currentBuild.currentResult)

        def previousBuild = currentBuild.previousBuild
        Result previousBuildResult = previousBuild ? Result.fromString(previousBuild.result) : null

        if( isBackToSuccess(buildResult, previousBuildResult) || isUnsuccessful(buildResult) ) {
            sendEmail(buildResult, previousBuildResult, configuration.recipients)
        }
    }
}


boolean isUnsuccessful(Result result) {
    return result == Result.UNSTABLE || result == Result.FAILURE
}

boolean isBackToSuccess(Result buildResult, Result previousBuildResult) {
    return buildResult == Result.SUCCESS && isUnsuccessful(previousBuildResult)
}

def sendEmail(Result buildStatus, Result previousStatus, List recipients){
    String subject = "Result of Job ${env.JOB_NAME} is ${buildStatus}"
    String body = "Build result of ${env.JOB_NAME} is ${buildStatus} (was ${previousStatus}). For more information, see: ${env.BUILD_URL}"
    String recipientsAsString = recipients.join(", ")

    emailext (recipientProviders: [[$class: 'CulpritsRecipientProvider']],
            to: recipientsAsString,
            subject: subject,
            body: body
    )
}
