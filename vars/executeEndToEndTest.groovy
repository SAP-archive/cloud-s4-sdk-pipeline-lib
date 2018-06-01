import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils
import com.sap.cloud.sdk.s4hana.pipeline.E2ETestCommandHelper
import com.sap.cloud.sdk.s4hana.pipeline.EndToEndTestType

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'executeEndToEndTest', stepParameters: parameters) {
        final script = parameters.script

        def appUrls = parameters.get('appUrls')
        EndToEndTestType type = parameters.get('endToEndTestType')
        def parallelE2ETests = [:]
        def index = 1

        def dockerOptions = ['--shm-size 512MB']
        DownloadCacheUtils.appendDownloadCacheNetworkOption(script, dockerOptions)

        if (appUrls) {
            for (def appUrl : appUrls) {

                String shScript
                List credentials = []

                if (appUrl instanceof String) {
                    shScript = E2ETestCommandHelper.generate(type, appUrl)
                } else if (appUrl instanceof Map && appUrl.url && appUrl.credentialId) {
                    String url = appUrl.url
                    shScript = E2ETestCommandHelper.generate(type, url)

                    String credentialId = appUrl.credentialId
                    credentials.add([$class: 'UsernamePasswordMultiBinding', credentialsId: credentialId, passwordVariable: 'e2e_password', usernameVariable: 'e2e_username'])
                } else {
                    error("Each appUrl in the configuration must be either a String or a Map containing a property url and a property credentialId.")
                }

                parallelE2ETests["E2E Tests ${index > 1 ? index : ''}"] = {
                    node(env.NODE_NAME) {
                        unstashFiles script: script, stage: parameters.stage
                        try {
                            withCredentials(credentials) {
                                executeNpm(script: script, dockerOptions: dockerOptions) {
                                    sh "Xvfb -ac :99 -screen 0 1280x1024x16 &"
                                    withEnv(['DISPLAY=:99']) {
                                        sh shScript
                                    }
                                }
                            }

                        } catch (Exception e) {
                            executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'End to End Tests', errorMessage: "Please examine End to End Test reports.") {
                                script.currentBuild.result = 'FAILURE'
                            }
                            throw e

                        } finally {
                            archive includes: "${s4SdkGlobals.endToEndReports}/**"
                            step($class: 'CucumberTestResultArchiver', testResults: "${s4SdkGlobals.endToEndReports}/*.json")
                            stashFiles script: script, stage: parameters.stage
                        }
                    }
                }
                index++
            }
            runClosures parallelE2ETests, script
        } else {
            echo "End to end test skipped because no appUrls defined!"
        }
    }
}
