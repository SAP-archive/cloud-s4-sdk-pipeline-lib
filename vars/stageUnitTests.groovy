import com.sap.icd.jenkins.ConfigurationLoader

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'executeUnitTests', stepParameters: parameters) {
        def script = parameters.script

        unstashFiles script: script, stage: 'unitTest'

        Map configuration = ConfigurationLoader.stageConfiguration(script, 'unitTests')

        try {
            executeMaven script: script, flags: '-U -B', pomPath: 'unit-tests/pom.xml', m2Path: s4SdkGlobals.m2Directory, goals: 'org.jacoco:jacoco-maven-plugin:0.7.9:prepare-agent test', dockerImage: configuration.dockerImage
        } catch(Exception e) {
            executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'Backend Unit Tests', errorMessage: "Build was ABORTED and marked as FAILURE, please examine Backend Unit Tests report.") {
                script.currentBuild.result = 'FAILURE'
            }
            throw e
        }
        finally {
            junit allowEmptyResults: true, testResults: 'unit-tests/target/surefire-reports/TEST-*.xml'
        }

        copyExecFile execFiles: [
            'unit-tests/target/jacoco.exec',
            'unit-tests/target/coverage-reports/jacoco.exec',
            'unit-tests/target/coverage-reports/jacoco-ut.exec'
        ], target: 'unit-tests.exec'

        stashFiles script: script, stage: 'unitTest'
    }
}




