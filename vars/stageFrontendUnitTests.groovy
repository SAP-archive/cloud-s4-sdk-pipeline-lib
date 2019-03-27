import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils
import com.sap.cloud.sdk.s4hana.pipeline.QualityCheck
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'frontendUnitTests'
    def script = parameters.script


    runAsStage(stageName: stageName, script: script) {
        Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)

        runOverModules(script: script, moduleType: 'html5') { basePath ->
            executeFrontendUnitTest(script, basePath, stageConfiguration)
        }
    }
}

private void executeFrontendUnitTest(def script, String basePath, Map stageConfiguration) {

    def dockerOptions = ['--cap-add=SYS_ADMIN']
    DownloadCacheUtils.appendDownloadCacheNetworkOption(script, dockerOptions)

    String packageJsonPath = basePath + '/package.json'

    dir(basePath) {
        if (fileExists(packageJsonPath)) {
            transformCiUnitTestScript(packageJsonPath)
            try {
                executeNpm(script: script, dockerImage: stageConfiguration?.dockerImage, dockerOptions: dockerOptions) {
                    sh "Xvfb -ac :99 -screen 0 1280x1024x16 &"
                    withEnv(['DISPLAY=:99']) {
                        sh "npm run ci-frontend-unit-test"
                    }
                }
            } catch (Exception e) {
                executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'Frontend Unit Tests', errorMessage: "Please examine Frontend Unit Tests report.") {
                    script.currentBuild.result = 'FAILURE'
                }
                throw e
            } finally {
                junit allowEmptyResults: true, testResults: 's4hana_pipeline/reports/frontend-unit/**/Test*.xml'

                publishHTML(target: [
                    allowMissing         : true,
                    alwaysLinkToLastBuild: false,
                    keepAll              : true,
                    reportDir            : script.s4SdkGlobals.frontendReports,
                    reportFiles          : 'index.html',
                    reportName           : "Frontend Unit Test Coverage"
                ])
            }
            ReportAggregator.instance.reportTestExecution(QualityCheck.FrontendUnitTests)
        } else {
            echo "Frontend unit tests skipped, because package.json does not exist!"
        }
    }
}

private void transformCiUnitTestScript(String packageJsonPath) {
    Map packageJson = readJSON file: packageJsonPath
    if (packageJson.scripts['ci-test']) {
        packageJson.scripts['ci-frontend-unit-test'] = packageJson.scripts['ci-test']
        packageJson.scripts.remove("ci-test")
        writeJSON json: packageJson, file: 'package.json'
        archiveArtifacts artifacts: 'package.json'
        echo "[WARNING]: You are using a legacy configuration parameter which might not be supported in the future in `package.json`. "
        "The npm-command `ci-test` is deprecated. Please rename it to `ci-frontend-unit-test` as shown in the `package.json` file in the build artifacts."
    }
}
