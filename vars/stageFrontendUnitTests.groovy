import com.sap.cloud.sdk.s4hana.pipeline.*
import com.sap.piper.ConfigurationLoader

import java.nio.file.Paths

def call(Map parameters = [:]) {
    def stageName = 'frontendUnitTests'
    def script = parameters.script

    piperStageWrapper(stageName: stageName, script: script) {
        Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)

        runOverModules(script: script, moduleType: 'html5') { basePath ->
            executeFrontendUnitTest(script, basePath, stageConfiguration)
        }
    }
}

private void executeFrontendUnitTest(Script script, String basePath, Map stageConfiguration) {

    String packageJsonPath = PathUtils.normalize(basePath, 'package.json')

    if (fileExists(packageJsonPath)) {
        def dockerOptions = ['--cap-add=SYS_ADMIN']
        DownloadCacheUtils.appendDownloadCacheNetworkOption(script, dockerOptions)
        NpmUtils.renameNpmScript(script, packageJsonPath, 'ci-test', 'ci-frontend-unit-test')
        try {
            dir(basePath) {
                executeNpm(script: script, dockerImage: stageConfiguration?.dockerImage, dockerOptions: dockerOptions) {
                    sh "Xvfb -ac :99 -screen 0 1280x1024x16 &"
                    withEnv(['DISPLAY=:99']) {
                        sh "npm run ci-frontend-unit-test"
                    }
                }
            }
        } catch (Exception e) {
            executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'Frontend Unit Tests', errorMessage: "Please examine Frontend Unit Tests report.") {
                script.currentBuild.result = 'FAILURE'
            }
            throw e
        } finally {
            // publish code coverage report for every module because HtmlPublisher cannot handle wildcards: https://issues.jenkins-ci.org/browse/JENKINS-7139
            publishCodeCoverageHtmlResult(basePath)
            junit allowEmptyResults: true, testResults: 's4hana_pipeline/reports/frontend-unit/**/Test*.xml'
            ReportAggregator.instance.reportTestExecution(QualityCheck.FrontendUnitTests)
        }
    } else {
        echo "Frontend unit tests skipped, because package.json does not exist at path '$basePath'. " +
            "If you want to execute frontend unit tests please ensure that a package.json with a script called 'ci-frontend-unit-test' is implemented." +
            "For more information about frontend unit tests please visit https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/doc/pipeline/build-tools.md#frontend-unit-tests"
    }
}

private void publishCodeCoverageHtmlResult(String basePath) {
    String legacyReportPath = Paths.get('s4hana_pipeline/reports/frontend-unit/coverage', basePath).normalize()
    String newCoverageReportPath = Paths.get(s4SdkGlobals.coverageReports, 'frontend-unit', basePath).normalize()
    //Fixme: Compatibility Layer in case coverage reports are stored in the old location.
    if(fileExists(legacyReportPath)){
        echo "[WARNING] Code coverage reports for frontend unit tests should be stored in ${newCoverageReportPath}."
        sh "mkdir -p ${newCoverageReportPath}"
        sh "cp -r ${legacyReportPath}/* ${newCoverageReportPath}"
    }

    publishHTML(target: [
        allowMissing         : true,
        alwaysLinkToLastBuild: false,
        keepAll              : true,
        reportDir            : "${newCoverageReportPath}/report-html/ut",
        reportFiles          : 'index.html',
        reportName           : "Frontend Unit Test Coverage ${BuildToolEnvironment.instance.isMta() ? basePath : ''}"
    ])
}
