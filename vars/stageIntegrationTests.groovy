import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationLoader
import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationMerger

def call(Map parameters = [:]) {
    def stageName = 'integrationTests'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {
        final Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)

        final Map stageDefaults = ConfigurationLoader.defaultStageConfiguration(script, stageName)

        List stageConfigurationKeys = [
            'retry',
            'credentials',
            'forkCount'
        ]

        Map configuration = ConfigurationMerger.merge(parameters, [], stageConfiguration, stageConfigurationKeys, stageDefaults)

        try {
            if(configuration.crendentials != null){
                configuration.credentials = configuration.crendentials
            }

            try {
                if(configuration.credentials != null){
                    dir("integration-tests/src/test/resources") { writeCredentials(configuration.credentials) }
                }

                int count
                try {
                    count = configuration.retry.toInteger()
                }
                catch(Exception e){
                    error ("retry: ${configuration.retry} must be an integer")
                }
                def forkCount = configuration.forkCount
                executeMaven script: script, flags: "-B", pomPath: "integration-tests/pom.xml", m2Path: s4SdkGlobals.m2Directory, goals: "org.jacoco:jacoco-maven-plugin:0.7.9:prepare-agent  test", dockerImage: configuration.dockerImage, defines: "-Dsurefire.rerunFailingTestsCount=$count -Dsurefire.forkCount=$forkCount"

            } catch(Exception e) {
                executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'Backend Integration Tests', errorMessage: "Please examine Backend Integration Tests report.") {
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
    }
}
