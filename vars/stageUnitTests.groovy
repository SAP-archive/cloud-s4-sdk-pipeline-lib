import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'unitTests'
    def script = parameters.script

    runAsStage(stageName: stageName, script: script) {
        Map configuration = ConfigurationLoader.stageConfiguration(script, stageName)

        runOverModules(script: script, moduleType: 'java') { basePath ->
            executeUnitTest(script, basePath, configuration)
        }

    }
}

private void executeUnitTest(def script, String basePath, Map configuration){
    try {
        String image = configuration.dockerImage

        mavenExecute(
            script: script,
            flags: "--batch-mode",
            pomPath: "${basePath}/unit-tests/pom.xml",
            m2Path: s4SdkGlobals.m2Directory,
            goals: "org.jacoco:jacoco-maven-plugin:prepare-agent test",
            dockerImage: image,
            defines: '-Dsurefire.forkCount=1C'
        )

    } catch (Exception e) {
        echo e.getLocalizedMessage()
        executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'Backend Unit Tests', errorMessage: "Please examine Backend Unit Tests report.") {
            script.currentBuild.result = 'FAILURE'
        }
        throw e
    }
    finally {
        junit allowEmptyResults: true, testResults: "${basePath}/unit-tests/target/surefire-reports/TEST-*.xml"
    }

    copyExecFile execFiles: [
        "${basePath}/unit-tests/target/jacoco.exec",
        "${basePath}/unit-tests/target/coverage-reports/jacoco.exec",
        "${basePath}/unit-tests/target/coverage-reports/jacoco-ut.exec"
    ], targetFolder:basePath, targetFile: 'unit-tests.exec'

    if (script.commonPipelineEnvironment.configuration.isMta) {
        sh("mkdir -p ${s4SdkGlobals.reportsDirectory}/service_audits/; cp $basePath/s4hana_pipeline/reports/service_audits/*.log ${s4SdkGlobals.reportsDirectory}/service_audits/ || echo 'Warning: No audit logs found'")
    }
}
