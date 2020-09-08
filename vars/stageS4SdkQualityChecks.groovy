import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationLoader

import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

def call(Map parameters = [:]) {
    def stageName = 's4SdkQualityChecks'
    def script = parameters.script
    piperStageWrapper(stageName: stageName, script: script) {
        Map stageConfiguration = loadEffectiveStageConfiguration(script: script, stageName: stageName)

        List disabledChecks = stageConfiguration.disabledChecks

        if (disabledChecks) {
            warnAboutDisabledChecks(disabledChecks)
        }

        if (BuildToolEnvironment.instance.isMaven() || BuildToolEnvironment.instance.isMta()) {
            if (!disabledChecks?.contains("checkDeploymentDescriptors")) {
                checkDeploymentDescriptors script: script
            }
        }

        if (!disabledChecks?.contains("checkFrontendCodeCoverage")) {
            checkFrontendCodeCoverage(
                script: script,
                codeCoverageFrontend: stageConfiguration.codeCoverageFrontend
            )
        }

        if (!disabledChecks?.contains("checkBackendCodeCoverage")) {
            checkBackendCodeCoverage(
                script: script,
                jacocoExcludes: stageConfiguration.jacocoExcludes,
                threshold: stageConfiguration.threshold
            )
        }

        visualizeCodeCoverageForJavaScript()
    }
}

def warnAboutDisabledChecks(List disabledChecks) {
    assertPluginIsActive('badge')
    addBadge(icon: "warning.gif", text: "You disabled the following quality checks: ${disabledChecks.join(", ")}, for details please have a look into the build summary or console output.")

    String html =
        """
<h2>Some checks in the stage Quality Checks are disabled</h2>
<p>The following checks are currently disabled in the pipeline:</p>
<ul>${disabledChecks.collect({ each -> "<li>$each</li>" }).join("")}</ul>
<p>Those quality checks support you to ensure qualities that are necessary in cloud-native environment.</p>
<p>To learn how to enable these checks, please consult the <a href="https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/configuration.md#s4sdkqualitychecks">documentation</a>.</p>
"""

    createSummary(icon: "warning.gif", text: html)
}
