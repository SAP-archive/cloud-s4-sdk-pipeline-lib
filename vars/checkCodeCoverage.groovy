import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationHelper
import groovy.transform.Field

import java.nio.file.Paths

import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

// Rules for the backend:
// Successful on line coverage >= 70
// Unstable on line coverage < 70
// Failure on line coverage < 65
//
// Jacoco note: Always set min. threshold parameter and max. threshold parameter of the jacoco plugin together.
// Because when min. threshold is configured and the max. threshold is not, by default max. is 0.
// Since min. is then greater than max., min. is then set to 0 automatically.
// As a result, both max. and min. are set to 0, which is the default value.
//
// For Javascript it is not possible to get code coverage results in Jacoco format.
// Therefore the Cobertura format is used and the Cobertura plugin to display the result and fail on thresholds.
// When setting Success and Unstable to the same value Cobertura will:
// Succeed on >= SuccessCodeCoverage, be unstable on < SuccessCodeCoverage and fail on < FailureCodeCoverage
//
// For the frontend the code coverage is configurable because we need no guarantees for further checks.
// Furthermore, the frontend could also be tested in the end to end tests.

@Field String STEP_NAME = 'checkCodeCoverage'

@Field Set PARAMETER_KEYS = [
    'jacocoExcludes',
    'codeCoverageFrontend'
]

def call(Map parameters = [:]) {
    handleStepErrors(stepName: STEP_NAME, stepParameters: parameters) {
        Script script = parameters.script

        Map configuration = ConfigurationHelper.newInstance(this)
            .loadStepDefaults()
            .mixin(parameters, PARAMETER_KEYS)
            .use()

        if (BuildToolEnvironment.instance.isMaven() || BuildToolEnvironment.instance.isMta()) {
            List jacocoExcludes = configuration.jacocoExcludes
            assertJacocoCodeCoverage(script, jacocoExcludes)
        } else if (BuildToolEnvironment.instance.isNpm()) {
            assertCodeCoverageJsBackend(script)
        }

        Map codeCoverageFrontend = configuration.codeCoverageFrontend
        assertCodeCoverageFrontend(script, codeCoverageFrontend)

        //Collect all coverage reports from backend and frontend in the cobertura format
        List coverageReports = findFiles(glob: "${s4SdkGlobals.coverageReports}/*.xml")
        if (coverageReports.size() > 0) {
            visualizeCodeCoverageForJavaScript()
        }
    }
}

private assertCodeCoverageJsBackend(Script script) {
    String backendUnitTestCoberturaReport = "${s4SdkGlobals.coverageReports}/backend-unit/cobertura-coverage.xml"
    String backendIntegrationTestCoberturaReport = "${s4SdkGlobals.coverageReports}/backend-integration/cobertura-coverage.xml"

    if (!fileExists(backendUnitTestCoberturaReport) || !fileExists(backendIntegrationTestCoberturaReport)) {
        error "Could not determine code coverage. " +
            "Please ensure the reports are generated as in cobertura format in the files `${s4SdkGlobals.coverageReports}/backend-unit/cobertura-coverage.xml`" +
            "and `${s4SdkGlobals.coverageReports}/backend-integration/cobertura-coverage.xml`. " +
            "If this should not happen, please open an issue at https://github.com/sap/cloud-s4-sdk-pipeline/issues and describe your project setup."
    }


    // The cobertura plugin can only handle multiple files if they are in a common directory. Therefore the reports are copied to a single directory
    sh "cp $backendUnitTestCoberturaReport ${s4SdkGlobals.coverageReports}/backend-unit-coverage.xml"
    sh "cp $backendIntegrationTestCoberturaReport ${s4SdkGlobals.coverageReports}/backend-integration-coverage.xml"

    String successBoundary = '70'
    String failureBoundary = '65'
    String unstableBoundary = '70'

    executeWithLockedCurrentBuildResult(
        script: script,
        errorStatus: 'FAILURE',
        errorHandler: script.buildFailureReason.setFailureReason,
        errorHandlerParameter: 'Check Code Coverage',
        errorMessage: "Please examine Code Coverage results."
    ) {
        assertPluginIsActive("cobertura")
        cobertura(autoUpdateHealth: false, autoUpdateStability: false,
            coberturaReportFile: "${s4SdkGlobals.coverageReports}/*.xml",
            failNoReports: false, failUnstable: false,
            lineCoverageTargets: "$successBoundary, $failureBoundary, $unstableBoundary",
            maxNumberOfBuilds: 0, onlyStable: false, zoomCoverageChart: false)
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

private assertJacocoCodeCoverage(Script script, List jacocoExcludes) {
    assertPluginIsActive("jacoco")
    def jacocoExclusionPattern

    String successCodeCoverage = '70'
    String unstableCodeCoverage = '65'

    jacocoExclusionPattern = jacocoExcludes == null ? '' : jacocoExcludes.join(',')
    executeWithLockedCurrentBuildResult(
        script: script,
        errorStatus: 'FAILURE',
        errorHandler: script.buildFailureReason.setFailureReason,
        errorHandlerParameter: 'Check Code Coverage',
        errorMessage: "Please examine the Code Coverage results. " +
            "There are either no tests or the test coverage is below the thresholds defined. " +
            "For more information please visit https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/doc/pipeline/cloud-qualities.md#code-coverage"
    ) {

        jacoco execPattern: "${s4SdkGlobals.coverageReports}/**/*.exec",
            exclusionPattern: "${jacocoExclusionPattern}",
            changeBuildStatus: true,
            maximumLineCoverage: successCodeCoverage,
            minimumLineCoverage: unstableCodeCoverage

        ReportAggregator.instance.reportCodeCoverageCheck(script, unstableCodeCoverage, jacocoExcludes)
    }
}

private visualizeCodeCoverageForJavaScript() {
    assertPluginIsActive("cobertura")
    cobertura(autoUpdateHealth: false, autoUpdateStability: false,
        coberturaReportFile: "${s4SdkGlobals.coverageReports}/*.xml",
        failNoReports: false, failUnstable: false,
        maxNumberOfBuilds: 0, onlyStable: false, zoomCoverageChart: false)

}
