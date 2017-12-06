import com.sap.icd.jenkins.ConfigurationLoader
import com.sap.icd.jenkins.ConfigurationHelper

def call(parameters = [:]) {
    handleStepErrors (stepName: 'notifyStatusChange', stepParameters: parameters) {
    def script = parameters.script


    def stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'feedback')
    def config = new ConfigurationHelper(stepConfiguration)
    def notificationRecipients = config.getConfigProperty('notificationRecipients', "nobody@nowhere.ci")
    def status
    def color
    switch (stepConfiguration.buildStatus) {
        case 'SUCCESS':
            status = 'is now green again!'
            color = 'good'
            break

        case 'UNSTABLE':
            status = 'has become unstable (usually test failed)'
            color = 'danger'
            break

        case 'FAILURE':
            status = 'has turned RED :('
            color = 'danger'
            break
    }

    emailext (
        subject: "${stepConfiguration.componentName} : Job '${env.JOB_NAME}' ${status}",
        body: "See ${env.BUILD_URL} for more details",
        recipientProviders: [
            [$class: 'DevelopersRecipientProvider'],
            [$class: 'CulpritsRecipientProvider'],
        ],
        to: notificationRecipients
    )

    if (config.isPropertyDefined("slack.channel")) {
        def channel = config.getConfigProperty("slack.channel")
        slackSend channel: channel, color: color, message: "${stepConfiguration.componentName} Build ${status}: ${currentBuild.fullDisplayName}, see : ${env.BUILD_URL}"
    }
}