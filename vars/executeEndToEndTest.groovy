import com.sap.icd.jenkins.E2ETestCommandHelper
import com.sap.icd.jenkins.EndToEndTestType

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'executeEndToEndTest',stepParameters: parameters) {
        final script = parameters.script

        def appUrls = parameters.get('appUrls')
        EndToEndTestType type = parameters.get('endToEndTestType')

        if(appUrls) {
            for(def appUrl : appUrls) {
                executeNpm(script: script) {
                    sh E2ETestCommandHelper.generate(type, appUrl)
                }
            }
        } else {
            echo "End to end test skipped because no appUrls defined!"
        }
    }
}