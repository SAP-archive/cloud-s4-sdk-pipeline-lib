import com.sap.piper.ConfigurationLoader

def call(Map parameters) {
    def script = parameters.script

    script.commonPipelineEnvironment.configuration.skipping = [:]

    if (fileExists('package.json')) {
        script.commonPipelineEnvironment.configuration.skipping.FRONT_END_BUILD = true
        script.commonPipelineEnvironment.configuration.skipping.FRONT_END_TESTS = true
    }
    if (ConfigurationLoader.stageConfiguration(script, 'endToEndTests') && script.commonPipelineEnvironment.configuration.skipping.FRONT_END_BUILD) {
        script.commonPipelineEnvironment.configuration.skipping.E2E_TESTS = true
    }
    if (ConfigurationLoader.stageConfiguration(script, 'performanceTests')) {
        script.commonPipelineEnvironment.configuration.skipping.PERFORMANCE_TESTS = true
    }

    if (script.commonPipelineEnvironment.configuration.skipping.E2E_TESTS || script.commonPipelineEnvironment.configuration.skipping.PERFORMANCE_TESTS) {
        script.commonPipelineEnvironment.configuration.skipping.REMOTE_TESTS = true
    }
    if (ConfigurationLoader.stageConfiguration(script, 'checkmarxScan') && isProductiveBranch(script: script)) {
        script.commonPipelineEnvironment.configuration.skipping.CHECKMARX_SCAN = true
    }

    boolean isWhitesourceConfigured =
        ConfigurationLoader.stageConfiguration(script, 'whitesourceScan')

    if (isProductiveBranch(script: script) && isWhitesourceConfigured) {
        script.commonPipelineEnvironment.configuration.skipping.WHITESOURCE_SCAN = true
    }

    if (fileExists('package.json')) {
        script.commonPipelineEnvironment.configuration.skipping.NODE_SECURITY_SCAN = true
    }

    if (ConfigurationLoader.stageConfiguration(script, 'sourceClearScan').credentialsId) {
        script.commonPipelineEnvironment.configuration.skipping.SOURCE_CLEAR_SCAN = true
    }

    if (script.commonPipelineEnvironment.configuration.skipping.CHECKMARX_SCAN
        || script.commonPipelineEnvironment.configuration.skipping.WHITESOURCE_SCAN
        || script.commonPipelineEnvironment.configuration.skipping.SOURCE_CLEAR_SCAN) {
        script.commonPipelineEnvironment.configuration.skipping.THIRD_PARTY_CHECKS = true
    }

    Map productionDeploymentConfiguration = ConfigurationLoader.stageConfiguration(script, 'productionDeployment')

    if ((productionDeploymentConfiguration.cfTargets || productionDeploymentConfiguration.neoTargets) && isProductiveBranch(script: script)) {
        script.commonPipelineEnvironment.configuration.skipping.PRODUCTION_DEPLOYMENT = true
    }

    if (ConfigurationLoader.stageConfiguration(script, 'artifactDeployment') && isProductiveBranch(script: script)) {
        script.commonPipelineEnvironment.configuration.skipping.ARTIFACT_DEPLOYMENT = true
    }

    def sendNotification = ConfigurationLoader.postActionConfiguration(script, 'sendNotification')
    if (sendNotification?.enabled && (!sendNotification.skipFeatureBranches || isProductiveBranch(script: script))) {
        script.commonPipelineEnvironment.configuration.skipping.SEND_NOTIFICATION = true
    }
}
