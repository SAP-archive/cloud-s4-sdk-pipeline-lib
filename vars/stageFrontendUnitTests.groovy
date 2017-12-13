import com.sap.icd.jenkins.ConfigurationLoader

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'stageFrontendUnitTests', stepParameters: parameters) {
        final script = parameters.script

        Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, 'frontendUnitTests')

        unstashFiles script: script, stage: 'frontendUnitTest'
        if(fileExists('package.json') ) {
            try {
                executeNpm(script: script, dockerImage: stageConfiguration?.dockerImage, dockerOptions: '--cap-add=SYS_ADMIN') {
                    sh "npm run ci-test -- --headless"
                }
            } catch(Exception e) {
                executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'Frontend Unit Tests', errorMessage: "Please examine Frontend Unit Tests report.") {
                    script.currentBuild.result = 'FAILURE'
                }
                throw e
            } finally{
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

        stashFiles script: script, stage: 'frontendUnitTest'
        echo "currentBuild.result: ${script.currentBuild.result}"
    }
}
