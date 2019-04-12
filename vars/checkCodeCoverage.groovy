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
            String coberturaReportFilename = "${s4SdkGlobals.coverageReports}/cobertura-coverage.xml"
            if (fileExists(coberturaReportFilename)) {
                //FIXME: because coverage reports are overwritten and therefore no real code coverage can be measured. 
                // Increase again as soon as the cobertura reports are handled in a correct manner.
                String successBoundary = '5'
                String failureBoundary = '1'
                String unstableBoundary = '5'

                executeWithLockedCurrentBuildResult(
                    script: script,
                    errorStatus: 'FAILURE',
                    errorHandler: script.buildFailureReason.setFailureReason,
                    errorHandlerParameter: 'Check Code Coverage',
                    errorMessage: "Please examine Code Coverage results."
                ) {

                    cobertura(autoUpdateHealth: false, autoUpdateStability: false,
                        coberturaReportFile: coberturaReportFilename,
                        failNoReports: false, failUnstable: false,
                        lineCoverageTargets: "$successBoundary, $failureBoundary, $unstableBoundary",
                        maxNumberOfBuilds: 0, onlyStable: false, zoomCoverageChart: false)
                }
            } else {
                error "Could not determine code coverage. " +
                    "Please ensure the report is generated as in cobertura format in the file `${s4SdkGlobals.coverageReports}/cobertura-coverage.xml`. " +
                    "If this should not happen, please open an issue at https://github.com/sap/cloud-s4-sdk-pipeline/issues and describe your project setup."
            }
        }
    }
}
