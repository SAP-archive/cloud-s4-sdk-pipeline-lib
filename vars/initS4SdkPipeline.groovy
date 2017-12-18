import com.sap.icd.jenkins.ConfigurationLoader

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'initS4SdkPipeline', stepParameters: parameters) {
        def script = parameters.script

        def mavenLocalRepository = new File(script.s4SdkGlobals.m2Directory)
        def reportsDirectory = new File(script.s4SdkGlobals.reportsDirectory)

        mavenLocalRepository.mkdirs()
        reportsDirectory.mkdirs()
        if (!fileExists(mavenLocalRepository.absolutePath) || !fileExists(reportsDirectory.absolutePath)) {
            errorWhenCurrentBuildResultIsWorseOrEqualTo(script: script, errorCurrentBuildStatus: 'FAILURE', errorMessage: "Please check if the user can create report directory.")
        }

        setupPipelineEnvironment(parameters)

        Map s4SdkStashConfiguration = readYaml(text: libraryResource('stash_settings.yml'))
        echo "Stash config: ${s4SdkStashConfiguration}"
        script.pipelineEnvironment.configuration.s4SdkStashConfiguration = s4SdkStashConfiguration

        if (!script.pipelineEnvironment.configuration.general.projectName?.trim() && fileExists('pom.xml')) {
            pom = readMavenPom file: 'pom.xml'
            script.pipelineEnvironment.configuration.general.projectName = pom.artifactId
        }
        def prefix = script.pipelineEnvironment.configuration.general.projectName
        script.pipelineEnvironment.configuration.currentBuildResultLock = "${prefix}/currentBuildResult"
        script.pipelineEnvironment.configuration.performanceTestLock = "${prefix}/performanceTest"
        script.pipelineEnvironment.configuration.endToEndTestLock = "${prefix}/endToEndTest"
        script.pipelineEnvironment.configuration.productionDeploymentLock = "${prefix}/productionDeployment"
        script.pipelineEnvironment.configuration.stashFiles = "${prefix}/stashFiles"
        initPipelineStageConfig(script)
        stashFiles script: script, stage: 'init'
    }
}

def initPipelineStageConfig(def script) {

    if (fileExists('package.json')) {
        script.pipelineEnvironment.skipConfiguration.FRONT_END_BUILD = true
        script.pipelineEnvironment.skipConfiguration.FRONT_END_TESTS = true
    }
    if (ConfigurationLoader.stageConfiguration(script, 'endToEndTests') && script.pipelineEnvironment.skipConfiguration.FRONT_END_BUILD) {
        script.pipelineEnvironment.skipConfiguration.E2E_TESTS = true
    }
    if (ConfigurationLoader.stageConfiguration(script, 'performanceTest')) {
        script.pipelineEnvironment.skipConfiguration.PERFORMANCE_TESTS = true
    }

    if (script.pipelineEnvironment.skipConfiguration.E2E_TESTS || script.pipelineEnvironment.skipConfiguration.PERFORMANCE_TESTS) {
        script.pipelineEnvironment.skipConfiguration.REMOTE_TESTS = true
    }
    if (ConfigurationLoader.stageConfiguration(script, 'checkmarxScan')) {
        script.pipelineEnvironment.skipConfiguration.CHECKMARX_SCAN = true
    }

    if (ConfigurationLoader.stageConfiguration(script, 'whitesourceScan') || fileExists('whitesource.config.json')) {
        script.pipelineEnvironment.skipConfiguration.WHITESOURCE_SCAN = true
    }

    if (ConfigurationLoader.stageConfiguration(script, 'nodeSecurityScan')?.enabled || fileExists('package.json')) {
        script.pipelineEnvironment.skipConfiguration.NODE_SECURITY_SCAN = true
    }

    if (script.pipelineEnvironment.skipConfiguration.CHECKMARX_SCAN
            || script.pipelineEnvironment.skipConfiguration.WHITESOURCE_SCAN
            || script.pipelineEnvironment.skipConfiguration.NODE_SECURITY_SCAN) {
        script.pipelineEnvironment.skipConfiguration.SECURITY_CHECKS = true
    }

    Map generalConfiguration = ConfigurationLoader.generalConfiguration(script)
    Map defaultGeneralConfiguration = ConfigurationLoader.defaultGeneralConfiguration(script)
    Map productionDeploymentConfiguration = ConfigurationLoader.stageConfiguration(script, 'productionDeployment')

    def productiveBranch = generalConfiguration.get('productiveBranch', defaultGeneralConfiguration.get('productiveBranch'))
    if ((productionDeploymentConfiguration.cfTargets || productionDeploymentConfiguration.neoTargets) && env.BRANCH_NAME == productiveBranch) {
        script.pipelineEnvironment.skipConfiguration.PRODUCTION_DEPLOYMENT = true
    }

    if (ConfigurationLoader.stageConfiguration(script, 'artifactDeployment') && env.BRANCH_NAME == productiveBranch) {
        script.pipelineEnvironment.skipConfiguration.ARTIFACT_DEPLOYMENT = true
    }

    def sendNotification = ConfigurationLoader.postActionConfiguration(script, 'sendNotification')
    if (sendNotification?.enabled && (!sendNotification.skipFeatureBranches || env.BRANCH_NAME == productiveBranch)){
        script.pipelineEnvironment.skipConfiguration.SEND_NOTIFICATION = true
    }

}
