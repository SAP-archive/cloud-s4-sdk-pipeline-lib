import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.NpmUtils
import com.sap.cloud.sdk.s4hana.pipeline.QualityCheck
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:]) {
    def stageName = 'backendIntegrationTests'
    def script = parameters.script

    final Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)
    final Map stageDefaults = ConfigurationLoader.defaultStageConfiguration(script, stageName)
    Set stageConfigurationKeys = [
        'retry',
        'credentials',
        'forkCount',
        'sidecarImage',
        'cloudFoundry',
        'createHdiContainer'
    ]
    Map configuration = ConfigurationMerger.merge(stageConfiguration, stageConfigurationKeys, stageDefaults)

    piperStageWrapper(stageName: stageName, script: script) {
        // The HDI container is cleaned up at the end of the execution
        createHdiContainer([script: script].plus(configuration)) {
            if (configuration.sidecarImage) {
                // Pass the env variable STAGE_NAME to dockerExecute to use the configuration of the stage
                withEnv(["STAGE_NAME=$stageName"]) {
                    executeIntegrationTest(script, stageName, configuration)
                }
            } else {
                executeIntegrationTest(script, stageName, configuration)
            }
        }
    }
}

private void executeIntegrationTest(def script, String stageName, Map configuration) {
    if (fileExists('integration-tests/pom.xml')) {
        javaIntegrationTests(script, configuration)
    }

    if (BuildToolEnvironment.instance.getNpmModulesWithScripts(['ci-integration-test', 'ci-it-backend'])) {
        jsIntegrationTests(script, configuration)
    }
}

private void jsIntegrationTests(Script script, Map configuration) {
    String credentialsFilePath = "./"

    writeTemporaryCredentials(configuration.credentials, credentialsFilePath) {
        Map executeNpmParameters = [script: script]

        // Disable the DL-cache in the integration-tests with sidecar with empty npm registry
        // This is necessary because it is currently not possible to not connect a container to multiple networks.
        //  FIXME: Remove when docker plugin supports multiple networks and jenkins-library implemented that feature
        try {
            if (configuration.sidecarImage) {

                Map executeNpmConfiguration = ConfigurationLoader.stepConfiguration(script, 'executeNpm')

                if (!executeNpmConfiguration.defaultNpmRegistry) {
                    executeNpmParameters.defaultNpmRegistry = ''
                }
            }
            String name = 'Backend Integration Tests'
            String pattern = 's4hana_pipeline/reports/backend-integration/**'

            def packageJsonFiles = findFiles(glob: '**/package.json', excludes: '**/node_modules/**')
            for (int i = 0; i < packageJsonFiles.size(); i++) {
                String packageJsonPath = (String) packageJsonFiles[i].path
                NpmUtils.renameNpmScript(script, packageJsonPath, 'ci-integration-test', 'ci-it-backend')
            }

            collectJUnitResults(script: script, testCategoryName: name, reportLocationPattern: pattern) {
                executeNpm(executeNpmParameters) {
                    runOverNpmModules(script: script, npmScripts: ['ci-it-backend']) { basePath ->
                        dir(basePath) {
                            sh "npm run ci-it-backend"
                        }
                    }
                }
            }
        } finally {
            archiveArtifacts artifacts: 's4hana_pipeline/reports/backend-integration/**', allowEmptyArchive: true
        }
    }
}

private void javaIntegrationTests(def script, Map configuration) {

    String credentialsFilePath = "integration-tests/src/test/resources"
    writeTemporaryCredentials(configuration.credentials, credentialsFilePath) {
        int count = 0
        try {
            count = configuration.retry.toInteger()
        }
        catch (Exception e) {
            error("retry: ${configuration.retry} must be an integer")
        }
        def forkCount = configuration.forkCount

        String pomPath = "integration-tests/pom.xml"

        injectQualityListenerDependencies(script: script, basePath: 'integration-tests')

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
        String testResultPattern = "integration-tests/target/surefire-reports/TEST-*.xml".replaceAll("//", "/")

        if (testResultPattern.startsWith("./")) {
            testResultPattern = testResultPattern.substring(2)
        }

        String pattern = 's4hana_pipeline/reports/backend-integration/**'
        collectJUnitResults(script: script, testCategoryName: name, reportLocationPattern: testResultPattern) {
            mavenExecute(mavenExecuteParameters)
        }

        ReportAggregator.instance.reportTestExecution(QualityCheck.BackendIntegrationTests)
    }

    copyExecFile execFiles: [
        "integration-tests/target/jacoco.exec",
        "integration-tests/target/coverage-reports/jacoco.exec",
        "integration-tests/target/coverage-reports/jacoco-ut.exec"
    ], targetFile: 'integration-tests.exec'

    if (BuildToolEnvironment.instance.isMta()) {
        sh("mkdir -p ${s4SdkGlobals.reportsDirectory}/service_audits/; cp s4hana_pipeline/reports/service_audits/*.log ${s4SdkGlobals.reportsDirectory}/service_audits/ || echo 'Warning: No audit logs found'")
    }
}
