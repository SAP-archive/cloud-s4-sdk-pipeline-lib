import com.sap.cloud.sdk.s4hana.pipeline.PathUtils
import com.sap.cloud.sdk.s4hana.pipeline.QualityCheck
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
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
        //Remove ./ in path as it does not work with surefire 3.0.0-M1
        String pomPath = PathUtils.normalize(basePath, "unit-tests/pom.xml")

        mavenExecute(
            script: script,
            flags: "--batch-mode",
            pomPath: pomPath,
            m2Path: s4SdkGlobals.m2Directory,
            goals: "org.jacoco:jacoco-maven-plugin:prepare-agent test",
            dockerImage: image,
            defines: '-Dsurefire.forkCount=1C'
        )
        ReportAggregator.instance.reportTestExecution(QualityCheck.UnitTests)

    } catch (Exception e) {
        echo e.getLocalizedMessage()
        executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'Backend Unit Tests', errorMessage: "Please examine Backend Unit Tests report.") {
            script.currentBuild.result = 'FAILURE'
        }
        throw e
    }
    finally {
        String testResultPattern = "${basePath}/unit-tests/target/surefire-reports/TEST-*.xml".replaceAll("//", "/")

        if(testResultPattern.startsWith("./")){
            testResultPattern = testResultPattern.substring(2)
        }

        junit allowEmptyResults: true, testResults: testResultPattern
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
