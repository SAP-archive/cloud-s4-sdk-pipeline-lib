import com.sap.icd.jenkins.ConfigurationLoader

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'initS4SdkPipeline', stepParameters: parameters) {
        def script = parameters.script

        def mavenLocalRepository = new File("${workspace}/${script.s4SdkGlobals.m2Directory}")
        def reportsDirectory = new File("${workspace}/${script.s4SdkGlobals.reportsDirectory}")

        mavenLocalRepository.mkdirs()
        reportsDirectory.mkdirs()
        if (!fileExists(mavenLocalRepository.absolutePath) || !fileExists(reportsDirectory.absolutePath)) {
            errorWhenCurrentBuildResultIsWorseOrEqualTo(script: script, errorCurrentBuildStatus: 'FAILURE', errorMessage: "Build was ABORTED and marked as FAILURE, please check if the user can create report directory.")
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
    return parameters.script.pipelineEnvironment.stageConfiguration
}

def initPipelineStageConfig(def script) {

    if (fileExists('package.json')) {
        script.pipelineEnvironment.stageConfiguration.FRONT_END_BUILD = true
        script.pipelineEnvironment.stageConfiguration.FRONT_END_TESTS = true
    }
    if (ConfigurationLoader.stageConfiguration(script, 'endToEndTests') && script.pipelineEnvironment.stageConfiguration.FRONT_END_BUILD) {
        script.pipelineEnvironment.stageConfiguration.E2E_TESTS = true
    }
    if (ConfigurationLoader.stageConfiguration(script, 'performanceTest')) {
        script.pipelineEnvironment.stageConfiguration.PERFORMANCE_TESTS = true
    }

    if (script.pipelineEnvironment.stageConfiguration.E2E_TESTS || script.pipelineEnvironment.stageConfiguration.PERFORMANCE_TESTS) {
        script.pipelineEnvironment.stageConfiguration.REMOTE_TESTS = true
    }
    if (ConfigurationLoader.stageConfiguration(script, 'checkmarxScan')) {
        script.pipelineEnvironment.stageConfiguration.CHECKMARX_SCAN = true
    }

    if (ConfigurationLoader.stageConfiguration(script, 'whitesourceScan') || fileExists('whitesource.config.json')) {
        script.pipelineEnvironment.stageConfiguration.WHITESOURCE_SCAN = true
    }

    if (ConfigurationLoader.stageConfiguration(script, 'nodeSecurityScan').enabled || fileExists('package.json')) {
        script.pipelineEnvironment.stageConfiguration.NODE_SECURITY_SCAN = true
    }

    if (script.pipelineEnvironment.stageConfiguration.CHECKMARX_SCAN
            || script.pipelineEnvironment.stageConfiguration.WHITESOURCE_SCAN
            || script.pipelineEnvironment.stageConfiguration.NODE_SECURITY_SCAN) {
        script.pipelineEnvironment.stageConfiguration.SECURITY_CHECKS = true
    }

    Map generalConfiguration = ConfigurationLoader.generalConfiguration(script)
    Map defaultGeneralConfiguration = ConfigurationLoader.defaultGeneralConfiguration(script)
    Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, 'productionDeployment')

    def productiveBranch = generalConfiguration.get('productiveBranch', defaultGeneralConfiguration.get('productiveBranch'))
    if ((stageConfiguration.cfTargets || stageConfiguration.neoTargets) && env.BRANCH_NAME == productiveBranch) {
        script.pipelineEnvironment.stageConfiguration.PRODUCTION_DEPLOYMENT = true
    }

    if (ConfigurationLoader.stageConfiguration(script, 'artifactDeployment')) {
        script.pipelineEnvironment.stageConfiguration.ARTIFACT_DEPLOYMENT = true
    }

}
