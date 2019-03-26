import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.piper.ConfigurationLoader

def call(Map parameters) {
    def script = parameters.script

    script.commonPipelineEnvironment.configuration.runStage = [:]

    if (BuildToolEnvironment.instance.isNpm()) {

        Map packageJson = readJSON file: 'package.json'
        Map npmScripts = packageJson.scripts

        if (npmScripts['ci-build']) {
            script.commonPipelineEnvironment.configuration.runStage.BUILD = true
        }

        if (npmScripts['ci-backend-unit-test']) {
            script.commonPipelineEnvironment.configuration.runStage.BACKEND_UNIT_TESTS = true
        }

        if (npmScripts['ci-frontend-unit-test']) {
            script.commonPipelineEnvironment.configuration.runStage.FRONTEND_UNIT_TESTS = true
        }

        if (npmScripts['ci-integration-test']) {
            script.commonPipelineEnvironment.configuration.runStage.INTEGRATION_TESTS = true
        }

        script.commonPipelineEnvironment.configuration.runStage.NPM_AUDIT = true
        // Activate ARCHIVE_REPORT when reporting is available for JS-Pipeline
        script.commonPipelineEnvironment.configuration.runStage.ARCHIVE_REPORT = false

    } else {
        script.commonPipelineEnvironment.configuration.runStage.BUILD = true
        script.commonPipelineEnvironment.configuration.runStage.BACKEND_UNIT_TESTS = true
        script.commonPipelineEnvironment.configuration.runStage.INTEGRATION_TESTS = true
        script.commonPipelineEnvironment.configuration.runStage.STATIC_CODE_CHECKS = true
        script.commonPipelineEnvironment.configuration.runStage.QUALITY_CHECKS = true
        script.commonPipelineEnvironment.configuration.runStage.ARCHIVE_REPORT = true

        if (fileExists('package.json')) {
            script.commonPipelineEnvironment.configuration.runStage.FRONT_END_TESTS = true
        }

        if (fileExists('package.json') && hasComponentJsFile()) {
            script.commonPipelineEnvironment.configuration.runStage.LINT = true
        }

        if (fileExists('package.json')) {
            script.commonPipelineEnvironment.configuration.runStage.NPM_AUDIT = true
        }
    }

    script.commonPipelineEnvironment.configuration.runStage.E2E_TESTS = endToEndTestsShouldRun(script)

    if (ConfigurationLoader.stageConfiguration(script, 'performanceTests')) {
        script.commonPipelineEnvironment.configuration.runStage.PERFORMANCE_TESTS = true
    }

    if (script.commonPipelineEnvironment.configuration.runStage.E2E_TESTS || script.commonPipelineEnvironment.configuration.runStage.PERFORMANCE_TESTS) {
        script.commonPipelineEnvironment.configuration.runStage.REMOTE_TESTS = true
    }
    if (ConfigurationLoader.stageConfiguration(script, 'checkmarxScan') && isProductiveBranch(script: script)) {
        script.commonPipelineEnvironment.configuration.runStage.CHECKMARX_SCAN = true
    }

    def projectInterceptorFile = "${s4SdkGlobals.projectExtensionsDirectory}/additionalTools.groovy"
    def repositoryInterceptorFile = "${s4SdkGlobals.repositoryExtensionsDirectory}/additionalTools.groovy"

    if ((fileExists(projectInterceptorFile) || fileExists(repositoryInterceptorFile))
        && isProductiveBranch(script: script)
        && ConfigurationLoader.stageConfiguration(script, 'additionalTools')) {
        script.commonPipelineEnvironment.configuration.runStage.ADDITIONAL_TOOLS = true
    }

    boolean isWhitesourceConfigured =
        ConfigurationLoader.stageConfiguration(script, 'whitesourceScan')

    if (isProductiveBranch(script: script) && isWhitesourceConfigured) {
        script.commonPipelineEnvironment.configuration.runStage.WHITESOURCE_SCAN = true
    }

    if (ConfigurationLoader.stageConfiguration(script, 'sourceClearScan').credentialsId) {
        script.commonPipelineEnvironment.configuration.runStage.SOURCE_CLEAR_SCAN = true
    }

    if (ConfigurationLoader.stageConfiguration(script, 'fortifyScan') && isProductiveBranch(script: script)) {
        script.commonPipelineEnvironment.configuration.runStage.FORTIFY_SCAN = true
    }

    if (script.commonPipelineEnvironment.configuration.runStage.CHECKMARX_SCAN
        || script.commonPipelineEnvironment.configuration.runStage.WHITESOURCE_SCAN
        || script.commonPipelineEnvironment.configuration.runStage.SOURCE_CLEAR_SCAN
        || script.commonPipelineEnvironment.configuration.runStage.FORTIFY_SCAN
        || script.commonPipelineEnvironment.configuration.runStage.ADDITIONAL_TOOLS) {
        script.commonPipelineEnvironment.configuration.runStage.THIRD_PARTY_CHECKS = true
    }

    Map productionDeploymentConfiguration = ConfigurationLoader.stageConfiguration(script, 'productionDeployment')

    if ((productionDeploymentConfiguration.cfTargets || productionDeploymentConfiguration.neoTargets) && isProductiveBranch(script: script)) {
        script.commonPipelineEnvironment.configuration.runStage.PRODUCTION_DEPLOYMENT = true
    }

    if (ConfigurationLoader.stageConfiguration(script, 'artifactDeployment') && isProductiveBranch(script: script)) {
        script.commonPipelineEnvironment.configuration.runStage.ARTIFACT_DEPLOYMENT = true
    }

    def sendNotification = ConfigurationLoader.postActionConfiguration(script, 'sendNotification')
    if (sendNotification?.enabled && (!sendNotification.skipFeatureBranches || isProductiveBranch(script: script))) {
        script.commonPipelineEnvironment.configuration.runStage.SEND_NOTIFICATION = true
    }
}

private boolean hasComponentJsFile() {
    String[] files = findFiles(glob: '**/Component.js')

    // Don't filter for node_modules or dist directory, as this does not exist at this stage in the pipeline.
    // In case this method is moved into checkUi5BestPractices, this case must be handled.

    if (files.size() > 1) {
        echo "Found multiple Component.js files, which is not supported. Found: ${files.join(", ")}. Skipping lint."
    }

    return files.size() == 1
}

private static boolean endToEndTestsShouldRun(script) {
    Map stageConfig = ConfigurationLoader.stageConfiguration(script, 'endToEndTests')

    if (!script.isProductiveBranch(script: script) && stageConfig?.onlyRunInProductiveBranch) {
        return false
    }

    return stageConfig && script.fileExists('package.json')
}
