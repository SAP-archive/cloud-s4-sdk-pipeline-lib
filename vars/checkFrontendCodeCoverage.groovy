import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationHelper
import groovy.transform.Field

import java.nio.file.Paths

import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

// For the frontend the code coverage is configurable because we need no guarantees for further checks.
// Furthermore, the frontend could also be tested in the end to end tests.

@Field String STEP_NAME = 'checkFrontendCodeCoverage'

@Field Set PARAMETER_KEYS = [
    'codeCoverageFrontend'
]

def call(Map parameters = [:]) {
    handleStepErrors(stepName: STEP_NAME, stepParameters: parameters) {
        Script script = parameters.script

        Map configuration = ConfigurationHelper.newInstance(this)
            .loadStepDefaults()
            .mixin(parameters, PARAMETER_KEYS)
            .use()

        Map codeCoverageFrontend = configuration.codeCoverageFrontend
        assertCodeCoverageFrontend(script, codeCoverageFrontend)
    }
}

private assertCodeCoverageFrontend(Script script, Map codeCoverageFrontend) {
    runOverModules(script: script, moduleType: 'html5') { basePath ->
        String coverageReportPath = Paths.get(s4SdkGlobals.coverageReports, 'frontend-unit', basePath, 'cobertura.frontend.unit.xml').normalize()
        if (fileExists(coverageReportPath)) {
            // The cobertura plugin can only handle multiple files if they are in a common directory. Therefore the reports are copied to a single directory
            String moduleName = Paths.get("./").getFileName().toString()
            if (moduleName == ".") {
                moduleName = 'root'
            }
            sh "cp ${coverageReportPath} ${s4SdkGlobals.coverageReports}/${moduleName}.cobertura.frontend.unit.xml"

            String successBoundary = codeCoverageFrontend.unstable
            String failureBoundary = codeCoverageFrontend.failing
            String unstableBoundary = codeCoverageFrontend.unstable

            executeWithLockedCurrentBuildResult(
                script: script,
                errorStatus: 'FAILURE',
                errorHandler: script.buildFailureReason.setFailureReason,
                errorHandlerParameter: 'Check Code Coverage',
                errorMessage: "Please examine Code Coverage results."
            ) {
                assertPluginIsActive("cobertura")
                cobertura(autoUpdateHealth: false, autoUpdateStability: false,
                    coberturaReportFile: "${s4SdkGlobals.coverageReports}/*.cobertura.frontend.unit.xml",
                    failNoReports: false, failUnstable: false,
                    lineCoverageTargets: "$successBoundary, $failureBoundary, $unstableBoundary",
                    maxNumberOfBuilds: 0, onlyStable: false, zoomCoverageChart: false)
            }
        } else {
            echo "[Warning] No code coverage file named cobertura.frontend.unit.xml found for frontend unit tests in path $basePath"
        }
    }
}
