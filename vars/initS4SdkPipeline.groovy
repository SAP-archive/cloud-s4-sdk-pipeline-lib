import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationLoader
import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationMerger
import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils

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
        if(DownloadCacheUtils.isCacheActive()) {
            echo "Download cache for maven and npm activated"

            writeFile file: "s4hana_pipeline/global_settings.xml", text: libraryResource("mvn_download_cache_proxy_settings.xml")

            // FIXME: Here we missuse the defaultConfiguration to control behavior in npm steps (will be merged with other values)
            script.pipelineEnvironment.defaultConfiguration.dockerNetwork = DownloadCacheUtils.networkName()

            // FIXME For maven we use the default settings (possible because executeMaven never sets any own dockerOptions)
            script.pipelineEnvironment.defaultConfiguration.steps.executeMaven.dockerOptions = DownloadCacheUtils.downloadCacheNetworkParam()

            script.pipelineEnvironment.defaultConfiguration.steps.executeMaven.globalSettingsFile="s4hana_pipeline/global_settings.xml"
            script.pipelineEnvironment.defaultConfiguration.steps.executeNpm.defaultNpmRegistry = "http://s4sdk-nexus:8081/repository/npm-proxy"

            if(script.pipelineEnvironment.configuration.steps?.executeNpm?.defaultNpmRegistry) {
                println("[WARNING]: Pipeline configuration contains custom value for 'executeNpm.defaultNpmRegistry'. "+
                    "The download cache will not be used for npm builds. To setup a npm-proxy, specify it in your 'server.cfg' file.")
            }

        }
        else {

            echo "Download cache for maven and npm not activated"
        }

        legacyConfigChecks(script.pipelineEnvironment.configuration)

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

        initStageSkipConfig(script)
        setAutoVersionIfOnProductiveBranch(script)
        stashFiles script: script, stage: 'init'
    }
}

def loadProductiveBranch(def script) {
    Map generalConfiguration = ConfigurationLoader.generalConfiguration(script)
    Map defaultGeneralConfiguration = ConfigurationLoader.defaultGeneralConfiguration(script)
    Map configWithDefault = ConfigurationMerger.merge(generalConfiguration, ['productiveBranch'], defaultGeneralConfiguration)
    return configWithDefault['productiveBranch']
}

def loadAutomaticVersioning(def script) {
    Map generalConfiguration = ConfigurationLoader.generalConfiguration(script)
    Map defaultGeneralConfiguration = ConfigurationLoader.defaultGeneralConfiguration(script)
    Map configWithDefault = ConfigurationMerger.merge(generalConfiguration, ['automaticVersioning'], defaultGeneralConfiguration)
    return configWithDefault['automaticVersioning']
}

def setAutoVersionIfOnProductiveBranch(def script) {
    String productiveBranch = loadProductiveBranch(script)
    boolean automaticVersioning = loadAutomaticVersioning(script)
    if (env.BRANCH_NAME == productiveBranch && automaticVersioning) {
        artifactSetVersion script: this, timestampTemplate: "%Y-%m-%dT%H%M%S%Z", buildTool: 'maven', commitVersion: false
    }
}

def initStageSkipConfig(def script) {

    if (fileExists('package.json')) {
        script.pipelineEnvironment.skipConfiguration.FRONT_END_BUILD = true
        script.pipelineEnvironment.skipConfiguration.FRONT_END_TESTS = true
    }
    if (ConfigurationLoader.stageConfiguration(script, 'endToEndTests') && script.pipelineEnvironment.skipConfiguration.FRONT_END_BUILD) {
        script.pipelineEnvironment.skipConfiguration.E2E_TESTS = true
    }
    if (ConfigurationLoader.stageConfiguration(script, 'performanceTests')) {
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

    if (fileExists('package.json')) {
        script.pipelineEnvironment.skipConfiguration.NODE_SECURITY_SCAN = true
    }

    if (script.pipelineEnvironment.skipConfiguration.CHECKMARX_SCAN
            || script.pipelineEnvironment.skipConfiguration.WHITESOURCE_SCAN) {
        script.pipelineEnvironment.skipConfiguration.THIRD_PARTY_CHECKS = true
    }

    Map productionDeploymentConfiguration = ConfigurationLoader.stageConfiguration(script, 'productionDeployment')

    def productiveBranch = loadProductiveBranch(script)
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

def legacyConfigChecks(Map configuration) {
    // Maven globalSettings obsolete since introduction of DL-Cache
    String globalSettingsFile = configuration?.steps?.executeMaven?.globalSettingsFile
    if(globalSettingsFile) {
        // switch to project settings if not also defined by user
        String projectSettingsFile = configuration?.steps?.executeMaven?.projectSettingsFile
        if(!projectSettingsFile) {
            println("[WARNING]: Your pipeline configuration contains the obsolete configuration parameter 'executeMaven.globalSettingsFile=${globalSettingsFile}'. "+
                "The S/4HANA Cloud SDK Pipeline uses an own global settings file to inject its download proxy as maven repository mirror. "+
                "Since you did not specify a project settings file, your settings file will be used as 'executeMaven.projectSettingsFile'.")
            configuration?.steps?.executeMaven?.projectSettingsFile=globalSettingsFile
            configuration.steps.executeMaven.remove('globalSettingsFile')
        }
        else {
            currentBuild.result = 'FAILURE'
            error("Your pipeline configuration contains the obsolete configuration parameter 'executeMaven.globalSettingsFile=${globalSettingsFile}' together with 'executeMaven.projectSettingsFile=${projectSettingsFile}'. "+
                "The S/4HANA Cloud SDK Pipeline uses an own global settings file to inject its download proxy as maven repository mirror. "+
                "Please reduce your settings to one file and specify it under 'executeMaven.globalSettingsFile'.")
        }
    }
}
