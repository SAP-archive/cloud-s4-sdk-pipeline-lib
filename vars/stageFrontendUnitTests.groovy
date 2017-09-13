import com.sap.icd.jenkins.ConfigurationLoader

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'stageFrontendUnitTests', stepParameters: parameters) {
        final script = parameters.script

        Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, 'functionalCorrectnessTest')

        unstashFiles script: script, stage: 'frontendUnitTest'
        if(fileExists('package.json') ) {
            executeNpm(script:script, dockerImage: stageConfiguration?.dockerImage, dockerOptions: '--cap-add=SYS_ADMIN') { sh "npm run ci-test -- --headless" }
        } else {
            echo "Frontend unit tests skipped, because package.json does not exist!"
        }
        stashFiles script: script, stage: 'frontendUnitTest'
    }
}