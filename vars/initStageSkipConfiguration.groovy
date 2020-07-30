import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment

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
        script.commonPipelineEnvironment.configuration.runStage.ARCHIVE_REPORT = true
    }

    if(fileExists('pom.xml')){
        script.commonPipelineEnvironment.configuration.runStage.STATIC_CODE_CHECKS = true
    }

    if (BuildToolEnvironment.instance.getNpmModules()) {
        script.commonPipelineEnvironment.configuration.runStage.NPM_AUDIT = true
    }

    def projectStageLintExtensionFile = "${s4SdkGlobals.projectExtensionsDirectory}/lint.groovy"
    def repositoryStageLintExtensionFile = "${s4SdkGlobals.repositoryExtensionsDirectory}/lint.groovy"

    List jsFiles = []
    List jsxFiles = []
    List tsFiles = []
    List tsxFiles = []

    try {
        jsFiles = findFiles(glob: '**/*.js', excludes: '**/node_modules/**,**/.*.js')
        jsxFiles = findFiles(glob: '**/*.jsx', excludes: '**/node_modules/**,**/.*.jsx')
        tsFiles = findFiles(glob: '**/*.ts', excludes: '**/node_modules/**,**/.*.ts')
        tsxFiles = findFiles(glob: '**/*.tsx', excludes: '**/node_modules/**,**/.*.tsx')
    } catch (IOException ioe) {
        echo "An error occurred when looking for js/ts files.\n" +
            "Exeption message: ${ioe.getMessage()}\n"
    }

    if (fileExists(projectStageLintExtensionFile) || fileExists(repositoryStageLintExtensionFile) || jsFiles.size() > 0 || jsxFiles.size() > 0 || tsFiles.size() > 0 || tsxFiles.size() > 0 ) {
        script.commonPipelineEnvironment.configuration.runStage.LINT = true
    } else {
        script.commonPipelineEnvironment.configuration.runStage.LINT = false
        echo "No Javascript/Typescript files or lint stage extensions found, skipping lint stage."
    }

    if (BuildToolEnvironment.instance.getNpmModulesWithScripts(['ci-test', 'ci-frontend-unit-test'])) {
        script.commonPipelineEnvironment.configuration.runStage.FRONTEND_UNIT_TESTS = true
    }

    script.commonPipelineEnvironment.configuration.runStage.QUALITY_CHECKS = true

    script.commonPipelineEnvironment.configuration.runStage.E2E_TESTS = endToEndTestsShouldRun(script)

    if (loadEffectiveStageConfiguration(script: script, stageName: 'performanceTests')) {
        script.commonPipelineEnvironment.configuration.runStage.PERFORMANCE_TESTS = true
    }

    if (script.commonPipelineEnvironment.configuration.runStage.E2E_TESTS || script.commonPipelineEnvironment.configuration.runStage.PERFORMANCE_TESTS) {
        script.commonPipelineEnvironment.configuration.runStage.REMOTE_TESTS = true
    }
    if (loadEffectiveStageConfiguration(script: script, stageName: 'checkmarxScan')?.groupId && isProductiveBranch(script: script)) {
        script.commonPipelineEnvironment.configuration.runStage.CHECKMARX_SCAN = true
    }

    Map sonarStageConfig = loadEffectiveStageConfiguration(script: script, stageName: 'sonarQubeScan')
    if (sonarStageConfig && (isProductiveBranch(script: script) || sonarStageConfig?.runInAllBranches)) {
        script.commonPipelineEnvironment.configuration.runStage.SONARQUBE_SCAN = true
    }

    def projectInterceptorFile = "${s4SdkGlobals.projectExtensionsDirectory}/additionalTools.groovy"
    def repositoryInterceptorFile = "${s4SdkGlobals.repositoryExtensionsDirectory}/additionalTools.groovy"

    if ((fileExists(projectInterceptorFile) || fileExists(repositoryInterceptorFile))
        && isProductiveBranch(script: script)
        && loadEffectiveStageConfiguration(script: script, stageName: 'additionalTools')) {
        script.commonPipelineEnvironment.configuration.runStage.ADDITIONAL_TOOLS = true
    }

    boolean isWhitesourceConfigured =
        loadEffectiveStageConfiguration(script: script, stageName: 'whitesourceScan')

    if (isProductiveBranch(script: script) && isWhitesourceConfigured) {
        script.commonPipelineEnvironment.configuration.runStage.WHITESOURCE_SCAN = true
    }

    if (loadEffectiveStepConfiguration(script: script, stepName: 'fortifyExecuteScan')?.fortifyCredentialsId && isProductiveBranch(script: script)) {
        script.commonPipelineEnvironment.configuration.runStage.FORTIFY_SCAN = true
    }

    if (loadEffectiveStepConfiguration(script: script, stepName: 'detectExecuteScan')?.detectTokenCredentialsId && isProductiveBranch(script: script)) {
        script.commonPipelineEnvironment.configuration.runStage.DETECT_SCAN = true
    }

    if (script.commonPipelineEnvironment.configuration.runStage.CHECKMARX_SCAN
        || script.commonPipelineEnvironment.configuration.runStage.WHITESOURCE_SCAN
        || script.commonPipelineEnvironment.configuration.runStage.SOURCE_CLEAR_SCAN
        || script.commonPipelineEnvironment.configuration.runStage.FORTIFY_SCAN
        || script.commonPipelineEnvironment.configuration.runStage.ADDITIONAL_TOOLS
        || script.commonPipelineEnvironment.configuration.runStage.SONARQUBE_SCAN
        || script.commonPipelineEnvironment.configuration.runStage.DETECT_SCAN){
        script.commonPipelineEnvironment.configuration.runStage.THIRD_PARTY_CHECKS = true
    }

    Map productionDeploymentConfiguration = loadEffectiveStageConfiguration(script: script, stageName: 'productionDeployment')

    if ((productionDeploymentConfiguration.cfTargets || productionDeploymentConfiguration.neoTargets || productionDeploymentConfiguration.tmsUpload) && isProductiveBranch(script: script)) {
        script.commonPipelineEnvironment.configuration.runStage.PRODUCTION_DEPLOYMENT = true
    }

    if (loadEffectiveStageConfiguration(script: script, stageName: 'artifactDeployment') && isProductiveBranch(script: script)) {
        script.commonPipelineEnvironment.configuration.runStage.ARTIFACT_DEPLOYMENT = true
    }

    def sendNotification = loadEffectivePostActionConfiguration(script: script, postAction: 'sendNotification')
    if (sendNotification?.enabled && (!sendNotification.skipFeatureBranches || isProductiveBranch(script: script))) {
        script.commonPipelineEnvironment.configuration.runStage.SEND_NOTIFICATION = true
    }

    if (loadEffectiveStageConfiguration(script: script, stageName: 'postPipelineHook')) {
        script.commonPipelineEnvironment.configuration.runStage.POST_PIPELINE_HOOK = true
    }
}

private boolean endToEndTestsShouldRun(script) {
    Map stageConfig = loadEffectiveStageConfiguration(script: script, stageName: "endToEndTests")

    if (!script.isProductiveBranch(script: script) && stageConfig?.onlyRunInProductiveBranch) {
        return false
    }

    return stageConfig.appUrls && script.fileExists('package.json')
}
