import com.sap.icd.jenkins.ConfigurationLoader

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'stageFrontendUnitTests', stepParameters: parameters) {
        final script = parameters.script

        Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, 'frontendUnitTests')

        unstashFiles script: script, stage: 'frontendUnitTest'
        if(fileExists('package.json') ) {
            executeNpm(script:script, dockerImage: stageConfiguration?.dockerImage, dockerOptions: '--cap-add=SYS_ADMIN') { sh "npm run ci-test -- --headless || echo 'Frontend Unit Tests Failed!'" }
        } else {
            echo "Frontend unit tests skipped, because package.json does not exist!"
        }

        executeWithLockedCurrentBuildResult(script: script, errorStatus: 'UNSTABLE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'Frontend Unit Tests', errorMessage: "Build was ABORTED and marked as FAILURE, please examine Frontend Unit Test reports."){
            junit allowEmptyResults: true, testResults: 's4hana_pipeline/reports/frontend-unit/**/Test*.xml'

            publishHTML(target: [
                allowMissing         : true,
                alwaysLinkToLastBuild: false,
                keepAll              : true,
                reportDir            : 's4hana_pipeline/reports/frontend-unit/coverage/report-html/ut',
                reportFiles          : 'index.html',
                reportName           : "Frontend Unit Test Coverage"
            ])
        }

        stashFiles script: script, stage: 'frontendUnitTest'
        echo "currentBuild.result: ${script.currentBuild.result}"
    }
}