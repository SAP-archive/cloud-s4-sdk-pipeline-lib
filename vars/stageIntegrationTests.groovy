import com.sap.icd.jenkins.ConfigurationLoader

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'stageIntegrationTests', stepParameters: parameters) {
        def script = parameters.script

        Map configuration = ConfigurationLoader.stageConfiguration(script, 'integrationTests')

        unstashFiles script: script, stage: 'integrationTest'

        try {
            if(configuration.crendentials != null){
                configuration.credentials = configuration.crendentials
            }

            if(configuration.credentials != null){
                dir("integration-tests/src/test/resources") { writeCredentials(configuration.credentials) }
            }
            try {
                executeMaven script: script, flags: "-U -B", pomPath: "integration-tests/pom.xml", m2Path: s4SdkGlobals.m2Directory, goals: "org.jacoco:jacoco-maven-plugin:0.7.9:prepare-agent  test", dockerImage: configuration.dockerImage
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
