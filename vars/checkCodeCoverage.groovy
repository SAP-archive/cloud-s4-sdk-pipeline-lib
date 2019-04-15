import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator

import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

// Rules:
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

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkCodeCoverage', stepParameters: parameters) {

        def script = parameters.script

        if (BuildToolEnvironment.instance.isMaven()) {
            assertPluginIsActive("jacoco")
            def jacocoExclusionPattern
            List jacocoExcludes = parameters.jacocoExcludes

            String successCodeCoverage = '70'
            String unstableCodeCoverage = '65'

            jacocoExclusionPattern = jacocoExcludes == null ? '' : jacocoExcludes.join(',')
            executeWithLockedCurrentBuildResult(
                script: script,
                errorStatus: 'FAILURE',
                errorHandler: script.buildFailureReason.setFailureReason,
                errorHandlerParameter: 'Check Code Coverage',
                errorMessage: "Please examine Code Coverage results."
            ) {

                jacoco execPattern: "${s4SdkGlobals.coverageReports}/**/*.exec",
                    exclusionPattern: "${jacocoExclusionPattern}",
                    changeBuildStatus: true,
                    maximumLineCoverage: successCodeCoverage,
                    minimumLineCoverage: unstableCodeCoverage

                ReportAggregator.instance.reportCodeCoverageCheck(script, unstableCodeCoverage, jacocoExcludes)
            }
        } else if (BuildToolEnvironment.instance.isNpm()) {
            assertPluginIsActive("cobertura")
            String backendUnitTestCoberturaReport = "${s4SdkGlobals.coverageReports}/backend-unit/cobertura-coverage.xml"
            String backendIntegrationTestCoberturaReport = "${s4SdkGlobals.coverageReports}/backend-integration/cobertura-coverage.xml"

            if (fileExists(backendUnitTestCoberturaReport) && fileExists(backendIntegrationTestCoberturaReport)) {
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

                    cobertura(autoUpdateHealth: false, autoUpdateStability: false,
                        coberturaReportFile: "${s4SdkGlobals.coverageReports}/*.xml",
                        failNoReports: false, failUnstable: false,
                        lineCoverageTargets: "$successBoundary, $failureBoundary, $unstableBoundary",
                        maxNumberOfBuilds: 0, onlyStable: false, zoomCoverageChart: false)
                }
            } else {
                error "Could not determine code coverage. " +
                    "Please ensure the reports are generated as in cobertura format in the files `${s4SdkGlobals.coverageReports}/backend-unit/cobertura-coverage.xml`" +
                    "and `${s4SdkGlobals.coverageReports}/backend-integration/cobertura-coverage.xml`. " +
                    "If this should not happen, please open an issue at https://github.com/sap/cloud-s4-sdk-pipeline/issues and describe your project setup."
            }
        }
    }
}
