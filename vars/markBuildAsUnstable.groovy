import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

/*
Wrap the `unstable` step (https://jenkins.io/blog/2019/07/05/jenkins-pipeline-stage-result-visualization-improvements/)
to provide additional badge info to make the reason more clear in the classic Jenkins ui.

As a fallback if the `unstable` step is not available, just set the build result.
 */
def call(Map parameters = [:]) {
    assertPluginIsActive('badge')

    def message = parameters.message
    def htmlFormattedMessage = parameters.htmlFormattedMessage
    try {
        unstable(message)
    } catch (NoSuchMethodError nsme) {
        echo message
        currentBuild.result = "UNSTABLE"
    }
    addBadge(icon: 'error.gif', text: message)
    createSummary(icon: 'error.gif', text: htmlFormattedMessage ?: message)
}
