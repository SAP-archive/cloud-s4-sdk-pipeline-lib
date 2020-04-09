import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils
import com.sap.cloud.sdk.s4hana.pipeline.WarningsUtils

import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

void call(Map parameters = [:]) {

    handleStepErrors(stepName: 'checkUserLint', stepParameters: parameters) {
        final script = parameters.script
        final Map configuration = parameters.configuration

        Map failThreshold = [:]

        List dockerOptions = []
        DownloadCacheUtils.appendDownloadCacheNetworkOption(script, dockerOptions)

        executeNpm(script: script, dockerImage: configuration?.dockerImage, dockerOptions: dockerOptions) {
            runOverNpmModules(script: script, npmScripts: ['ci-lint']) { basePath ->
                dir(basePath) {
                    int status = sh(script: "npm run --silent ci-lint", returnStatus: true)
                }
            }
        }
        executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'Linter', errorMessage: "User defined ci-lint failed. Please fix the reported violations.") {
            WarningsUtils.createLintingResultsReport(script, "userLint", "Linter", "**/*cilint.xml", failThreshold)
        }

    }
}
