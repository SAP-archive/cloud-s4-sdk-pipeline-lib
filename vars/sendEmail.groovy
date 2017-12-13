import com.sap.icd.jenkins.ConfigurationLoader
import com.sap.icd.jenkins.ConfigurationMerger

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'sendEmail', stepParameters: parameters) {
        def script = parameters.script

        final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'sendEmail')

        List stepConfigurationKeys = ['recipients']

        List parameterKeys = ['buildStatus']

        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys)

        def recipients = ''
        if(configuration.recipients) {
            recipients = configuration.recipients.get(configuration.buildStatus.toString())
        }
        emailTo(configuration.buildStatus, recipients)
    }
}

def emailTo(buildStatus, recipients){
    def body = "Build result of ${JOB_NAME} is ${buildStatus}, see : ${BUILD_URL}"
    def subject = "$JOB_NAME $buildStatus"
    emailext recipientProviders: [[$class: 'CulpritsRecipientProvider']],
            to: recipients,
            subject: subject,
            body: body
}
