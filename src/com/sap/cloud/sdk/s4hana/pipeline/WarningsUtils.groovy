package com.sap.cloud.sdk.s4hana.pipeline

import com.sap.piper.JenkinsUtils
import hudson.PluginWrapper

import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

class WarningsUtils implements Serializable {
    static final long serialVersionUID = 1L

    static final String pluginName = 'warnings-ng'

    static void addPipelineWarning(Script script, String heading, String message) {
        script.echo '[WARNING]: ' + message
        assertPluginIsActive('badge')
        script.addBadge(icon: "warning.gif", text: message)

        String html =
            """
<h2>$heading</h2>
<p>$message</p>
"""

        script.createSummary(icon: "warning.gif", text: html)
    }

    static void createLintingResultsReport(Script script, String scanToolId, String scanToolName, String scanToolPattern, Map failThreshold){
        int failedError = (failThreshold?.error != null) ? failThreshold.error : Integer.MAX_VALUE
        int failedNormal = (failThreshold?.warning != null) ? failThreshold.warning : Integer.MAX_VALUE
        int failedLow = (failThreshold?.info != null) ? failThreshold.info : Integer.MAX_VALUE

        assertPluginIsActive(pluginName)

        Map recordIssuesParams = [
            blameDisabled: true,
            enabledForFailure: true,
            aggregatingResults: false,
            tool: script.checkStyle(id: scanToolId, name: scanToolName, pattern: scanToolPattern),
            qualityGates: [
                [threshold: failedError, type: 'TOTAL_ERROR', unstable: false],
                [threshold: failedNormal, type: 'TOTAL_NORMAL', unstable: false],
                [threshold: failedLow, type: 'TOTAL_LOW', unstable: false],
            ]
        ]
        recordIssuesParams = addSkipPublishingChecksIfNecessary(script, recordIssuesParams)

        script.recordIssues recordIssuesParams

        // run-ui5-lint script creates *.json and *.xml files, whereas ci-lint and the default are expected to only
        // output a single *.xml file
        if (scanToolId == "ui5lint"){
            script.sh 'rm -f *.ui5lint.*'
        } else {
            script.sh "rm -f ${scanToolPattern}"
        }
    }

    /**
     * If the plugin version is >= 8.4.x, adds the property 'skipPublishingChecks'
     * to suppress the feature, which requires the Jenkins Root URL to be configured.
     * See discussion in https://github.com/jenkinsci/warnings-ng-plugin/pull/550 for
     * more info.
     */
    static private Map addSkipPublishingChecksIfNecessary(Script script, Map parameters) {
        String[] versionFields
        try {
            PluginWrapper pluginWrapper = JenkinsUtils.isPluginActive(pluginName)
            versionFields = pluginWrapper.version.split('\\.')
            if (versionFields.length > 1
                && Integer.valueOf(versionFields[0]) >= 8
                && Integer.valueOf(versionFields[1]) >= 4) {
                parameters.skipPublishingChecks = true
            }
        } catch (Throwable t) {
            script.echo "failed to test version (${versionFields}) of plugin '$pluginName': $t"
        }
        return parameters
    }
}
