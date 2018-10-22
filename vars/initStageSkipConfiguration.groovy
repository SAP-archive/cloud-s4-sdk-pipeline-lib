import com.sap.piper.ConfigurationLoader

def call(Map parameters) {
    def script = parameters.script

    script.commonPipelineEnvironment.configuration.runStage = [:]

    if (fileExists('package.json')) {
        script.commonPipelineEnvironment.configuration.runStage.FRONT_END_BUILD = true
        script.commonPipelineEnvironment.configuration.runStage.FRONT_END_TESTS = true
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

    if (fileExists('package.json')) {
        script.commonPipelineEnvironment.configuration.runStage.NODE_SECURITY_SCAN = true
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

private static boolean endToEndTestsShouldRun(script) {
    Map stageConfig = ConfigurationLoader.stageConfiguration(script, 'endToEndTests')

    if (!script.isProductiveBranch(script: script) && stageConfig?.onlyRunInProductiveBranch) {
        return false
    }

    return stageConfig && script.commonPipelineEnvironment.configuration.runStage.FRONT_END_BUILD
}
