import com.sap.cloud.sdk.s4hana.pipeline.E2ETestCommandHelper
import com.sap.cloud.sdk.s4hana.pipeline.EndToEndTestType

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'executeEndToEndTest',stepParameters: parameters) {
        final script = parameters.script

        def appUrls = parameters.get('appUrls')
        EndToEndTestType type = parameters.get('endToEndTestType')

        if(appUrls) {
            for(def appUrl : appUrls) {

                String shScript
                List credentials = []

                if(appUrl instanceof String){
                    shScript = E2ETestCommandHelper.generate(type, appUrl)
                }
                else if(appUrl instanceof Map && appUrl.url && appUrl.credentialId) {
                    String url = appUrl.url
                    shScript = E2ETestCommandHelper.generate(type, url)

                    String credentialId = appUrl.credentialId
                    credentials.add([$class: 'UsernamePasswordMultiBinding', credentialsId: credentialId, passwordVariable: 'e2e_password', usernameVariable: 'e2e_username'])
                }
                else {
                    error("Each appUrl in the configuration must be either a String or a Map containing a property url and a property credentialId.")
                }

                try {
                    withCredentials(credentials) {
                        executeNpm(script: script, dockerOptions:'--shm-size 512MB') {
                            sh "Xvfb -ac :99 -screen 0 1280x1024x16 &"
                            withEnv(['DISPLAY=:99']) {
                                sh shScript
                            }
                        }
                    }

                } catch(Exception e) {
                    executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'End to End Tests', errorMessage: "Please examine End to End Test reports.") {
                        script.currentBuild.result = 'FAILURE'
                    }
                    throw e

                } finally{
                    archive includes: "${s4SdkGlobals.endToEndReports}/**"
                    step($class: 'CucumberTestResultArchiver', testResults: "${s4SdkGlobals.endToEndReports}/*.json")
                }
            }
        } else {
            echo "End to end test skipped because no appUrls defined!"
        }
    }
}
