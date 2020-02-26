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
}
