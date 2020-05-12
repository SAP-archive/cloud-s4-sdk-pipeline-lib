import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils
import com.sap.cloud.sdk.s4hana.pipeline.WarningsUtils

import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

void call(Map parameters = [:]) {

    handleStepErrors(stepName: 'checkDefaultLint', stepParameters: parameters) {
        final script = parameters.script
        final Map configuration = parameters.configuration

        Map failThreshold = [:]
        List eslintConfigs = []

        List dockerOptions = []
        DownloadCacheUtils.appendDownloadCacheNetworkOption(script, dockerOptions)

        try {
            eslintConfigs = findFiles(glob: '**/.eslintrc*', excludes: '**/node_modules/**')
        } catch (IOException ioe) {
            error "An error occurred when looking for eslint config.\n" +
                "Exeption message: ${ioe.getMessage()}\n"
        }
        int status
        executeNpm(script: script, dockerImage: configuration?.dockerImage, dockerOptions: dockerOptions) {
            // npm install seems to be necessary, since ESLint is not able to find plugins installed by npx on the fly
            // and placed in npm cache
            status = sh(script: "npm install eslint@latest typescript@latest @typescript-eslint/parser@latest @typescript-eslint/eslint-plugin@latest", returnStatus: true)

            // If the user has ESLint configs in the project we use them to lint existing JS files. In this case we do not lint other types of files,
            // i.e., .jsx, .ts, .tsx, since we can not be sure that the provided config enables parsing of these file types.
            if (eslintConfigs.size() > 0){
                for (int i = 0; i < eslintConfigs.size(); i++) {
                    String eslintConfigPath = eslintConfigs[i].path.minus(eslintConfigs[i].name)

                    // If the eslintConfigPath is empty, then ESLint config resides in root directory of the project
                    if (!eslintConfigPath){
                        // For the execution we rely on ESLint's built-in mechanism to determine which configuration to use for which JS files
                        // More information on that can be found here: https://eslint.org/docs/user-guide/configuring#configuration-cascading-and-hierarchy
                        status = sh(script: "npx --no-install eslint . -f checkstyle -o ./${i}_defaultlint.xml --ignore-pattern node_modules/", returnStatus: true)
                    } else {
                        status = sh(script: "npx --no-install eslint ${eslintConfigPath}**/*.js -f checkstyle -o ./${i}_defaultlint.xml --ignore-pattern node_modules/", returnStatus: true)
                    }

                    // From ESLint docs: Exit codes: 2: Linting was unsuccessful due to a configuration problem or an internal error.
                    if (status == 2) {
                        echo "ESLint returned exit code 2. There might be a problem with your ESLint configuration or ESLint had an internal error."
                        echo "Please ensure that your ESLint configuration is correct. If the error persists, please file a ticket at: https://github.com/SAP/cloud-s4-sdk-pipeline/issues/new/choose"
                    }
                }
            } else {
                String eslintConfigDefault = libraryResource ".eslintrc.json"
                writeFile file: ".eslintrc.json", text: eslintConfigDefault

                // Here we can use the --ext .js,.jsx,.ts,.tsx option, since our general purpose config provides the necessary configuration to parse and process all kinds of JS/TS files
                status = sh(script: "npx --no-install eslint . --ext .js,.jsx,.ts,.tsx -c ./.eslintrc.json -f checkstyle -o ./defaultlint.xml --ignore-pattern node_modules/", returnStatus: true)
            }
        }
        executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'Linter', errorMessage: "ESLint failed. Please fix the reported violations.") {
            WarningsUtils.createLintingResultsReport(script, "defaultLint", "Linter", "*defaultlint.xml", failThreshold)
        }
    }
}
