import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.piper.ConfigurationLoader

def call(Map parameters) {
    def script = parameters.script
    Map packageJson
    Map npmScripts

    script.commonPipelineEnvironment.configuration.runStage = [:]

    // Check for test scripts in all npm package.json files in the project, as they don't have to be implemented in the top-level file
    if (BuildToolEnvironment.instance.npmModules) {

        if (BuildToolEnvironment.instance.getNpmModulesWithScripts(['ci-it-frontend'])) {
            script.commonPipelineEnvironment.configuration.runStage.FRONTEND_INTEGRATION_TESTS = true
        }
        if (BuildToolEnvironment.instance.getNpmModulesWithScripts(['ci-backend-unit-test'])) {
            script.commonPipelineEnvironment.configuration.runStage.BACKEND_UNIT_TESTS = true
        }

        if (BuildToolEnvironment.instance.getNpmModulesWithScripts(['ci-integration-test', 'ci-it-backend'])) {
            script.commonPipelineEnvironment.configuration.runStage.BACKEND_INTEGRATION_TESTS = true
        }
    }

    if (BuildToolEnvironment.instance.isNpm()) {
        // Check for "build" only in top-level package json, as one repo typically contains one deployable unit
        packageJson = readJSON file: 'package.json'
        npmScripts = packageJson.scripts

        if (npmScripts['ci-build']) {
            script.commonPipelineEnvironment.configuration.runStage.BUILD = true
        }

        //TODO Activate ARCHIVE_REPORT when reporting is available for JS-Pipeline
        script.commonPipelineEnvironment.configuration.runStage.ARCHIVE_REPORT = false

    } else {
        script.commonPipelineEnvironment.configuration.runStage.BUILD = true
        script.commonPipelineEnvironment.configuration.runStage.BACKEND_UNIT_TESTS = true
        if (BuildToolEnvironment.instance.isMtaWithIntegrationTests(script) || BuildToolEnvironment.instance.isMaven()) {
            script.commonPipelineEnvironment.configuration.runStage.BACKEND_INTEGRATION_TESTS = true
        }
        script.commonPipelineEnvironment.configuration.runStage.STATIC_CODE_CHECKS = true
        script.commonPipelineEnvironment.configuration.runStage.ARCHIVE_REPORT = true
    }

    if (BuildToolEnvironment.instance.getNpmModules()) {
        script.commonPipelineEnvironment.configuration.runStage.NPM_AUDIT = true
    }

    // Always run by default, but allow disabling via extension
    script.commonPipelineEnvironment.configuration.runStage.LINT = true

    if (BuildToolEnvironment.instance.getNpmModulesWithScripts(['ci-test', 'ci-frontend-unit-test'])) {
        script.commonPipelineEnvironment.configuration.runStage.FRONTEND_UNIT_TESTS = true
    }

    script.commonPipelineEnvironment.configuration.runStage.QUALITY_CHECKS = true

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

    if (ConfigurationLoader.stageConfiguration(script, 'sonarQubeScan') && isProductiveBranch(script: script)) {
        script.commonPipelineEnvironment.configuration.runStage.SONARQUBE_SCAN = true
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
        || script.commonPipelineEnvironment.configuration.runStage.ADDITIONAL_TOOLS
        || script.commonPipelineEnvironment.configuration.runStage.SONARQUBE_SCAN) {
        script.commonPipelineEnvironment.configuration.runStage.THIRD_PARTY_CHECKS = true
    }

    Map productionDeploymentConfiguration = ConfigurationLoader.stageConfiguration(script, 'productionDeployment')

    if ((productionDeploymentConfiguration.cfTargets || productionDeploymentConfiguration.neoTargets || productionDeploymentConfiguration.tmsUpload) && isProductiveBranch(script: script)) {
        script.commonPipelineEnvironment.configuration.runStage.PRODUCTION_DEPLOYMENT = true
    }

    if (ConfigurationLoader.stageConfiguration(script, 'artifactDeployment') && isProductiveBranch(script: script)) {
        script.commonPipelineEnvironment.configuration.runStage.ARTIFACT_DEPLOYMENT = true
    }

    def sendNotification = ConfigurationLoader.postActionConfiguration(script, 'sendNotification')
    if (sendNotification?.enabled && (!sendNotification.skipFeatureBranches || isProductiveBranch(script: script))) {
        script.commonPipelineEnvironment.configuration.runStage.SEND_NOTIFICATION = true
    }

    if (ConfigurationLoader.stageConfiguration(script, 'postPipelineHook')) {
        script.commonPipelineEnvironment.configuration.runStage.POST_PIPELINE_HOOK = true
    }
}

private static boolean endToEndTestsShouldRun(script) {
    Map stageConfig = ConfigurationLoader.stageConfiguration(script, 'endToEndTests')

    if (!script.isProductiveBranch(script: script) && stageConfig?.onlyRunInProductiveBranch) {
        return false
    }

    return stageConfig && script.fileExists('package.json')
}
