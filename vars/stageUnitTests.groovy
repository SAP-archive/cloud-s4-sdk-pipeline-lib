import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.PathUtils
import com.sap.cloud.sdk.s4hana.pipeline.QualityCheck
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'unitTests'
    def script = parameters.script

    runAsStage(stageName: stageName, script: script) {
        Map configuration = ConfigurationLoader.stageConfiguration(script, stageName)

        if (BuildToolEnvironment.instance.isNpm()) {
            executeJsUnitTest(script)
        } else {
            runOverModules(script: script, moduleType: 'java') { basePath ->
                executeJavaUnitTest(script, basePath, configuration)
            }
        }
    }
}

private void executeJsUnitTest(def script) {

    String name = 'Backend Unit Tests'
    String pattern = 's4hana_pipeline/reports/backend-unit/**'
    collectJUnitResults(script: script, testCategoryName: name, reportLocationPattern: pattern) {
        executeNpm(script: script, dockerOptions: []) {
            sh "npm run ci-backend-unit-test"
        }
    }
}

private void executeJavaUnitTest(def script, String basePath, Map configuration) {

    String image = configuration.dockerImage
    //Remove ./ in path as it does not work with surefire 3.0.0-M1
    String pomPath = PathUtils.normalize(basePath, "unit-tests/pom.xml")

    String name = 'Backend Unit Tests'

    String pattern = "${basePath}/unit-tests/target/surefire-reports/TEST-*.xml".replaceAll("//", "/")

    if(pattern.startsWith("./")){
        pattern = pattern.substring(2)
    }

    collectJUnitResults(script: script, testCategoryName: name, reportLocationPattern: pattern) {
        mavenExecute(
            script: script,
            flags: "--batch-mode",
            pomPath: pomPath,
            m2Path: s4SdkGlobals.m2Directory,
            goals: "org.jacoco:jacoco-maven-plugin:prepare-agent test",
            dockerImage: image,
            defines: '-Dsurefire.forkCount=1C'
        )
    }

    ReportAggregator.instance.reportTestExecution(QualityCheck.UnitTests)

    copyExecFile execFiles: [
        "${basePath}/unit-tests/target/jacoco.exec",
        "${basePath}/unit-tests/target/coverage-reports/jacoco.exec",
        "${basePath}/unit-tests/target/coverage-reports/jacoco-ut.exec"
    ], targetFolder:basePath, targetFile: 'unit-tests.exec'

    if (BuildToolEnvironment.instance.isMta()) {
        sh("mkdir -p ${s4SdkGlobals.reportsDirectory}/service_audits/; cp $basePath/s4hana_pipeline/reports/service_audits/*.log ${s4SdkGlobals.reportsDirectory}/service_audits/ || echo 'Warning: No audit logs found'")
    }
}
