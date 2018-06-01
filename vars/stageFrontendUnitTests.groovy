import com.sap.piper.ConfigurationLoader
import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils

def call(Map parameters = [:]) {
    def stageName = 'frontendUnitTests'
    def script = parameters.script

    runAsStage(stageName: stageName, script: script) {
        Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)

        def dockerOptions = ['--cap-add=SYS_ADMIN']
        DownloadCacheUtils.appendDownloadCacheNetworkOption(script, dockerOptions)

        if (fileExists('package.json')) {
            try {
                executeNpm(script: script, dockerImage: stageConfiguration?.dockerImage, dockerOptions: dockerOptions) {
                    sh "Xvfb -ac :99 -screen 0 1280x1024x16 &"
                    withEnv(['DISPLAY=:99']) {
                        sh "npm run ci-test"
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
        } else {
            echo "Frontend unit tests skipped, because package.json does not exist!"
        }
    }
}
