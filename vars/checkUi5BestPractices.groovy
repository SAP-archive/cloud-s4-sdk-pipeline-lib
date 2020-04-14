import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils
import com.sap.cloud.sdk.s4hana.pipeline.WarningsUtils
import com.sap.piper.BashUtils

import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

void call(Map parameters = [:]) {

    handleStepErrors(stepName: 'checkUi5BestPractices', stepParameters: parameters) {

        String[] ui5Components = parameters.ui5Components

        echo "UI5 Best Practices lint discovered ${ui5Components.size()} UI5 components for scan: ${ui5Components.join(", ")}."

        final script = parameters.script
        final Map configuration = parameters.configuration

        List dockerOptions = []
        DownloadCacheUtils.appendDownloadCacheNetworkOption(script, dockerOptions)

        String checkUi5BestPracticesScript = libraryResource "check-ui5-best-practices/run-ui5-lint.js"
        writeFile file: "run-ui5-lint.js", text: checkUi5BestPracticesScript

        String packageJson = libraryResource "check-ui5-best-practices/package.json"
        writeFile file: "package.json", text: packageJson

        executeNpm(script: script, dockerOptions: dockerOptions) {
            sh 'npm config set @sap:registry https://npm.sap.com'
            sh 'npm install'

            // deprecated property to enable es6 -> Use esLanguageLevel instead
            boolean es6Enabled = configuration?.ui5BestPractices?.enableES6
            String esLanguageLevel = configuration?.ui5BestPractices?.esLanguageLevel

            if (!esLanguageLevel) {
                if (es6Enabled) {
                    WarningsUtils.addPipelineWarning(script, "Deprecated option es6Enabled used", "Please use the esLanguageLevel setting for ui5BestPractices which supports multiple language levels.")
                    esLanguageLevel = 'es6'
                }
            }

            ui5Components.each { componentJsFile ->
                if (esLanguageLevel) {
                    sh "node run-ui5-lint.js ${BashUtils.quoteAndEscape(componentJsFile)} ${esLanguageLevel} "
                } else {
                    sh "node run-ui5-lint.js ${BashUtils.quoteAndEscape(componentJsFile)} "
                }
            }
        }

        executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'checkUi5BestPractices', errorMessage: "SAPUI5 lint failed. Please fix the reported violations.") {
            WarningsUtils.createLintingResultsReport(script, "ui5lint", "SAPUI5 Best Practices", '*ui5lint.xml' , configuration?.ui5BestPractices?.failThreshold)
        }
    }
}
