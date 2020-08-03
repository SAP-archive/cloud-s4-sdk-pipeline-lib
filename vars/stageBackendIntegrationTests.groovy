import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.NpmUtils
import com.sap.cloud.sdk.s4hana.pipeline.QualityCheck
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'backendIntegrationTests'
    def script = parameters.script

    Set stageConfigurationKeys = [
        'retry',
        'credentials',
        'forkCount',
        'sidecarImage',
        'cloudFoundry',
        'createHdiContainer'
    ]

    Map configuration = loadEffectiveStageConfiguration(script: script, stageName: stageName, stageConfigurationKeys: stageConfigurationKeys)

    piperStageWrapper(stageName: stageName, script: script) {
        // The HDI container is cleaned up at the end of the execution
        createHdiContainer([script: script].plus(configuration)) {
            // Pass the env variable STAGE_NAME
            // 1.) In case sideCar is configured, so dockerExecute uses the configuration of the stage
            // 2.) So the piper binary pulls the correct stage configuration
            withEnv(["STAGE_NAME=$stageName"]) {
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

        injectQualityListenerDependencies(script: script, basePath: 'integration-tests')

        String name = 'Backend Integration Tests'

        // NOTE: The pattern must not start with "./" and must not contain double slashes "//".
        String testResultPattern = 'integration-tests/target/surefire-reports/TEST-*.xml'

        collectJUnitResults(script: script, testCategoryName: name, reportLocationPattern: testResultPattern) {
            mavenExecuteIntegration(script: script)
        }

        ReportAggregator.instance.reportTestExecution(QualityCheck.BackendIntegrationTests)
    }

    if (BuildToolEnvironment.instance.isMta()) {
        sh("mkdir -p ${s4SdkGlobals.reportsDirectory}/service_audits/; cp s4hana_pipeline/reports/service_audits/*.log ${s4SdkGlobals.reportsDirectory}/service_audits/ || echo 'Warning: No audit logs found'")
    }
}
