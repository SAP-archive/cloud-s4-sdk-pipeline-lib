package com.sap.cloud.sdk.s4hana.pipeline

import com.cloudbees.groovy.cps.NonCPS
import com.sap.piper.JenkinsUtils

class EnvironmentAssertionUtils implements Serializable {
    static final long serialVersionUID = 1L

    @NonCPS
    static void assertPluginIsActive(String pluginName) {
        if (pluginName == null || pluginName.empty) {
            throw new RuntimeException("Plugin name cannot be null or empty.")
        }

        if (!JenkinsUtils.isPluginActive(pluginName)) {
            String exception = """[ERROR] Plugin '${pluginName}' is not installed or not active.
            | Please update the Jenkins image to the latest available version. 
            | For more information how to update the image please visit:
            | https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/doc/operations/operations-guide.md#update-image
            | """.stripMargin().stripIndent()
            throw new RuntimeException(exception)
        }
    }
}
