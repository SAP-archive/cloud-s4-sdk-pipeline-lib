import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.PathUtils
import com.sap.cloud.sdk.s4hana.pipeline.QualityCheck
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:]) {
    def stageName = 'integrationTests'
    def script = parameters.script

    runAsStage(stageName: stageName, script: script) {
        runOverModules(script: script, moduleType: "java") { String basePath ->
            executeIntegrationTest(script, basePath, stageName)
        }
    }
}

private void executeIntegrationTest(def script, String basePath, String stageName) {
    final Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)
    final Map stageDefaults = ConfigurationLoader.defaultStageConfiguration(script, stageName)
    Set stageConfigurationKeys = [
        'retry',
        'credentials',
        'forkCount',
        'sidecarImage'
    ]
    Map configuration = ConfigurationMerger.merge(stageConfiguration, stageConfigurationKeys, stageDefaults)

    Closure integrationTests
    if (BuildToolEnvironment.instance.isNpm()) {
        integrationTests = jsIntegrationTests(script, configuration)
    } else {
        integrationTests = javaIntegrationTests(script, configuration, basePath)
    }

    if (configuration.sidecarImage) {
        // Pass the env variable STAGE_NAME to dockerExecute to use the configuration of the stage
        withEnv(["STAGE_NAME=$stageName"]) {
            integrationTests()
        }
    } else {
        integrationTests()
    }
}

private Closure jsIntegrationTests(def script, Map configuration) {
    return {
        String credentialsFilePath = "./"

        writeTemporaryCredentials(configuration.credentials, credentialsFilePath) {
            Map executeNpmParameters = [script: script]

            // Disable the DL-cache in the integration-tests with sidecar with empty npm registry
            // This is necessary because it is currently not possible to not connect a container to multiple networks.
            //  FIXME: Remove when docker plugin supports multiple networks and jenkins-library implemented that feature
            if (configuration.sidecarImage) {

                Map executeNpmConfiguration = ConfigurationLoader.stepConfiguration(script, 'executeNpm')

                if (!executeNpmConfiguration.defaultNpmRegistry) {
                    executeNpmParameters.defaultNpmRegistry = ''
                }
            }
            String name = 'Backend Integration Tests'
            String pattern = 's4hana_pipeline/reports/backend-integration/**'
            collectJUnitResults(script: script, testCategoryName: name, reportLocationPattern: pattern) {
                executeNpm(executeNpmParameters) {
                    sh "npm run ci-integration-test"
                }
            }
        }
    }
}

private Closure javaIntegrationTests(def script, Map configuration, String basePath) {
    return {

        String credentialsFilePath = "$basePath/integration-tests/src/test/resources"
        writeTemporaryCredentials(configuration.credentials, credentialsFilePath) {
            int count = 0
            try {
                count = configuration.retry.toInteger()
            }
            catch (Exception e) {
                error("retry: ${configuration.retry} must be an integer")
            }
            def forkCount = configuration.forkCount

            //Remove ./ in path as it does not work with surefire 3.0.0-M1
            String pomPath = PathUtils.normalize(basePath, "integration-tests/pom.xml")

            Map mavenExecuteParameters = [
                script     : script,
                flags      : "--batch-mode",
                pomPath    : pomPath,
                m2Path     : s4SdkGlobals.m2Directory,
                goals      : "org.jacoco:jacoco-maven-plugin:prepare-agent test",
                dockerImage: configuration.dockerImage,
                defines    : "-Dsurefire.rerunFailingTestsCount=$count -Dsurefire.forkCount=$forkCount"
            ]

            // Disable the DL-cache in the integration-tests with sidecar with empty docker options and no global settings file for maven
            // This is necessary because it is currently not possible to not connect a container to multiple networks.
            //  FIXME: Remove when docker plugin supports multiple networks and jenkins-library implemented that feature
            if (configuration.sidecarImage) {
                mavenExecuteParameters.dockerOptions = []
                mavenExecuteParameters.globalSettingsFile = ''
            }

            String name = 'Backend Integration Tests'
            String testResultPattern = "${basePath}/integration-tests/target/surefire-reports/TEST-*.xml".replaceAll("//", "/")

            if (testResultPattern.startsWith("./")) {
                testResultPattern = testResultPattern.substring(2)
            }

            String pattern = 's4hana_pipeline/reports/backend-integration/**'
            collectJUnitResults(script: script, testCategoryName: name, reportLocationPattern: testResultPattern) {
                mavenExecute(mavenExecuteParameters)
            }

            ReportAggregator.instance.reportTestExecution(QualityCheck.IntegrationTests)
        }

        copyExecFile execFiles: [
            "$basePath/integration-tests/target/jacoco.exec",
            "$basePath/integration-tests/target/coverage-reports/jacoco.exec",
            "$basePath/integration-tests/target/coverage-reports/jacoco-ut.exec"
        ], targetFolder: basePath, targetFile: 'integration-tests.exec'

        if (BuildToolEnvironment.instance.isMta()) {
            sh("mkdir -p ${s4SdkGlobals.reportsDirectory}/service_audits/; cp $basePath/s4hana_pipeline/reports/service_audits/*.log ${s4SdkGlobals.reportsDirectory}/service_audits/ || echo 'Warning: No audit logs found'")
        }
    }
}
