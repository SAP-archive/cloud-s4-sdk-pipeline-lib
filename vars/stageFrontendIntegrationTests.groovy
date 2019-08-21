import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils
import com.sap.cloud.sdk.s4hana.pipeline.QualityCheck
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'frontendIntegrationTests'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {
        Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)
        executeFrontendIntegrationTest(script, stageConfiguration)
    }
}

private void executeFrontendIntegrationTest(def script, Map stageConfiguration) {
    String name = 'Frontend Integration Tests'
    String pattern = 's4hana_pipeline/reports/frontend-integration/**/*.xml'

    def dockerOptions = ['--cap-add=SYS_ADMIN']
    DownloadCacheUtils.appendDownloadCacheNetworkOption(script, dockerOptions)

    collectJUnitResults(script: script, testCategoryName: name, reportLocationPattern: pattern) {
        executeNpm(script: script, dockerImage: stageConfiguration?.dockerImage, dockerOptions: dockerOptions) {
            sh "Xvfb -ac :99 -screen 0 1280x1024x16 &"
            withEnv(['DISPLAY=:99']) {
                sh "npm run ci-it-frontend"
            }
        }
        ReportAggregator.instance.reportTestExecution(QualityCheck.FrontendIntegrationTests)
    }
    archiveArtifacts artifacts: 's4hana_pipeline/reports/frontend-integration/**', allowEmptyArchive: true
}
