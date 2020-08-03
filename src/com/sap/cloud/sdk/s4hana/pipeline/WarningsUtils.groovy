package com.sap.cloud.sdk.s4hana.pipeline

import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

class WarningsUtils implements Serializable {
    static final long serialVersionUID = 1L

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

        assertPluginIsActive('warnings-ng')
        script.recordIssues blameDisabled: true,
            enabledForFailure: true,
            aggregatingResults: false,
            skipPublishingChecks: true,
            tool: script.checkStyle(id: scanToolId, name: scanToolName, pattern: scanToolPattern),
            qualityGates: [
                [threshold: failedError, type: 'TOTAL_ERROR', unstable: false],
                [threshold: failedNormal, type: 'TOTAL_NORMAL', unstable: false],
                [threshold: failedLow, type: 'TOTAL_LOW', unstable: false],
            ]
        // run-ui5-lint script creates *.json and *.xml files, whereas ci-lint and the default are expected to only
        // output a single *.xml file
        if (scanToolId == "ui5lint"){
            script.sh 'rm -f *.ui5lint.*'
        } else {
            script.sh "rm -f ${scanToolPattern}"
        }
    }
}
