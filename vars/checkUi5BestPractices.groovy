import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils

import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

def call(Map parameters = [:]) {

    handleStepErrors(stepName: 'checkUi5BestPractices', stepParameters: parameters) {
        assertPluginIsActive('warnings-ng')
        assertPluginIsActive('badge')

        final script = parameters.script
        final Map configuration = parameters.configuration
        final String basePath = parameters.basePath

        dir(basePath) {
            // In case a Gruntfile already exists, replace it with a well-known Gruntfile just for this step
            if (fileExists('Gruntfile.js')) {
                sh 'rm -f Gruntfile.js'
            }
            String gruntFileContent = libraryResource resource: 'checkUi5BestPractices_Gruntfile_js'
            writeFile file: 'Gruntfile.js', text: gruntFileContent

            def dockerOptions = []
            DownloadCacheUtils.appendDownloadCacheNetworkOption(script, dockerOptions)
            executeNpm(script: script, dockerOptions: dockerOptions) {
                sh 'npm install grunt grunt-cli'
                sh 'npm config set @sap:registry https://npm.sap.com'
                sh 'npm install @sap/grunt-sapui5-bestpractice-build --save-dev'
                sh 'node_modules/.bin/grunt'
            }

            final String gruntBestPracticesScanResultsFile = 'dist/di.code-validation.core_issues.json'
            if (fileExists(gruntBestPracticesScanResultsFile)) {
                executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'checkUi5BestPractices', errorMessage: "SAPUI5 lint failed. Please fix the reported violations.") {
                    recordDiscoveredIssues(gruntBestPracticesScanResultsFile, configuration?.ui5BestPractices?.failThreshold)
                }
            } else {
                createSummary(icon: "warning.gif", text: "<h2>Lint skipped</h2>" +
                    "Could not find file <pre>${gruntBestPracticesScanResultsFile}</pre>, skipping SAPUI5 best practices lint")
                addBadge(icon: "warning.gif", text: "Could not read file ${gruntBestPracticesScanResultsFile}, skip SAPUI5 best practices lint.")
                echo "Could not find file ${gruntBestPracticesScanResultsFile}, skipping SAPUI5 best practices lint."
            }
        }
    }
}

private void recordDiscoveredIssues(String gruntBestPracticesScanResultsFile, Map failThreshold) {
    def issues = readJSON file: gruntBestPracticesScanResultsFile

    String checkstyleScanResult = '<?xml version="1.0" encoding="UTF-8"?>\n<checkstyle version="8.16">\n'

    Map xmlFilesWithIssues = issues.results['@sap/di.code-validation.xml'].'issues'
    checkstyleScanResult += buildCheckstyleResultXmlForIssues(xmlFilesWithIssues)

    Map jsFilesWithIssues = issues.results['@sap/di.code-validation.js'].'issues'
    checkstyleScanResult += buildCheckstyleResultXmlForIssues(jsFilesWithIssues)

    Map jsonFilesWithIssues = issues.results['@sap/di.code-validation.json'].'issues'
    checkstyleScanResult += buildCheckstyleResultXmlForIssues(jsonFilesWithIssues)

    checkstyleScanResult += '</checkstyle>\n'

    writeFile file: 'ui5-best-practices-scan-results.xml', text: checkstyleScanResult

    // Threshold of zero disables the check, see here for reference:
    // https://github.com/jenkinsci/warnings-ng-plugin/blob/aeefd58196ab304b6b35ef7ec330c4ba30f16e4c/src/main/java/io/jenkins/plugins/analysis/core/util/ThresholdSet.java#L74
    int failedHigh = (failThreshold?.error != null) ? failThreshold.error : 0
    int failedNormal = (failThreshold?.warning != null) ? failThreshold.warning : 0
    int failedLow = (failThreshold?.info != null) ? failThreshold.info : 0

    recordIssues failedTotalHigh: failedHigh,
        failedTotalNormal: failedNormal,
        failedTotalLow: failedLow,
        blameDisabled: true,
        enabledForFailure: true,
        aggregatingResults: false,
        tool: checkStyle(pattern: 'ui5-best-practices-scan-results.xml')

    sh 'rm -f ui5-best-practices-scan-results.xml'
}

private String buildCheckstyleResultXmlForIssues(Map issues) {
    String result = ""

    List filesWithFindings = issues.keySet().asList()

    for (int i = 0; i < filesWithFindings.size(); i++) {
        result += "  <file name=\"${findFilePath(filesWithFindings[i])}\">\n"
        List violations = issues[filesWithFindings[i]]

        for (int j = 0; j < violations.size(); j++) {
            result += "    <error line=\"${violations[j].line}\" severity=\"${violations[j].severity}\" message=\"${violations[j].message}\" source=\"${violations[j].source}\"/>\n"
        }
        result += '  </file>\n'

    }
    return result
}

// Sadly, we don't get the path to the scanned file relative to the project root by the grunt build, thus we have to search for it in the workspace.
private String findFilePath(knownPathFragment) {
    Object[] files = findFiles(glob: "**/${knownPathFragment}")

    if (files.length == 1) {
        String relativePath = files[0].path
        def workspacePath = pwd()
        if (fileExists(relativePath)) {
            return "${workspacePath}/${relativePath}"
        } else {
            error "File ${workspacePath}/${relativePath} does not exist. " +
                "Please report this unexpected issue including a stack trace and information on your project structure at https://github.com/sap/cloud-s4-sdk-pipeline/issues (be sure to omit any sensitive information)."
        }
    } else {
        error "Could not uniquely locate file ${knownPathFragment} in the workspace. Found: ${files.join(", ")}. " +
            "Please report this unexpected issue including a stack trace and information on your project structure at https://github.com/sap/cloud-s4-sdk-pipeline/issues (be sure to omit any sensitive information)."
    }
}
