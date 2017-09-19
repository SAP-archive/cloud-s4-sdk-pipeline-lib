import com.sap.icd.jenkins.ConfigurationLoader
import com.sap.icd.jenkins.ConfigurationMerger

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'executeEndToEndTest',stepParameters: parameters) {
        final script = parameters.script

        def appUrl = parameters.get('appUrl')
        if(appUrl) {
            executeNpm(script: script){ // The "--" says the following args will be passed to the script ci-e2e.
                sh "npm run ci-e2e -- --headless --launchUrl=${appUrl}" }
        } else {
            echo "End to end test skipped because no appUrl defined!"
        }
    }
}