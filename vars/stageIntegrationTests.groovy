import com.sap.icd.jenkins.ConfigurationLoader
import com.sap.icd.jenkins.ConfigurationMerger

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'stageIntegrationTests', stepParameters: parameters) {
        def script = parameters.script

        final Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, 'integrationTests')

        final Map stageDefaults = ConfigurationLoader.defaultStageConfiguration(script, 'integrationTests')

        List stageConfigurationKeys = [
                'retry',
                'credentials'
        ]

        Map configuration = ConfigurationMerger.merge(parameters, [], stageConfiguration, stageConfigurationKeys, stageDefaults)

        unstashFiles script: script, stage: 'integrationTest'

        try {
            if(configuration.crendentials != null){
                configuration.credentials = configuration.crendentials
            }

            if(configuration.credentials != null){
                dir("integration-tests/src/test/resources") { writeCredentials(configuration.credentials) }
            }

            try {
                int count
                try{
                    count = configuration.retry.toInteger()
                } catch(Exception e){
                    error ("retry: ${retry} must be an integer")
                }
                retry(count) {
                    executeMaven script: script, flags: "-U -B", pomPath: "integration-tests/pom.xml", m2Path: s4SdkGlobals.m2Directory, goals: "org.jacoco:jacoco-maven-plugin:0.7.9:prepare-agent  test", dockerImage: configuration.dockerImage
                }
            } catch(Exception e) {
                executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'Backend Integration Tests', errorMessage: "Build was ABORTED and marked as FAILURE, please examine Backend Integration Tests report.") {
                    script.currentBuild.result = 'FAILURE'
                }
                throw e
            }
            finally {
                junit allowEmptyResults: true, testResults: 'integration-tests/target/surefire-reports/TEST-*.xml'
            }
        }
        finally {
            dir("integration-tests/src/test/resources") { deleteCredentials() }
        }

        copyExecFile execFiles: [
            'integration-tests/target/jacoco.exec',
            'integration-tests/target/coverage-reports/jacoco.exec',
            'integration-tests/target/coverage-reports/jacoco-ut.exec'
        ], target: 'integration-tests.exec'

        stashFiles script: script, stage: 'integrationTest'
    }
}
