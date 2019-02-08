import com.cloudbees.groovy.cps.NonCPS
import com.sap.cloud.sdk.s4hana.pipeline.Analytics
import com.sap.cloud.sdk.s4hana.pipeline.MavenUtils
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator

def call(Map parameters) {
    def stageName = 'initS4sdkPipeline'
    def script = parameters.script

    loadPiper script: script

    /*
    In order to avoid the trust issues between the build server and the git server in a distributed setup,
    the init stage always executes on the master node. The underlying assumption here is that, Jenkins
    server has a ssh key and it has been added to the git server. This is necessary if Jenkins has to push
    code changes to the git server.
    */
    runAsStage(stageName: stageName, script: script, node: 'master') {
        Map scmCheckoutResult = checkout scm

        if(scmCheckoutResult.GIT_COMMIT){
            ReportAggregator.instance.reportVersionControlUsed('Git')
        }

        initS4SdkPipelineLibrary script: script
        initStashConfiguration script: script

        Analytics.instance.initAnalytics(script)

        String extensionRepository = script.loadEffectiveGeneralConfiguration(script: script).extensionRepository
        if (extensionRepository != null) {
            try {
                sh "git clone --depth 1 ${extensionRepository} ${s4SdkGlobals.repositoryExtensionsDirectory}"
            } catch (Exception e) {
                error("Error while executing git clone when accessing repository ${extensionRepository}.")
            }
        }
        loadAdditionalLibraries script: script

        def mavenLocalRepository = new File(script.s4SdkGlobals.m2Directory)
        def reportsDirectory = new File(script.s4SdkGlobals.reportsDirectory)

        mavenLocalRepository.mkdirs()
        reportsDirectory.mkdirs()
        if (!fileExists(mavenLocalRepository.absolutePath) || !fileExists(reportsDirectory.absolutePath)) {
            errorWhenCurrentBuildResultIsWorseOrEqualTo(
                script: script,
                errorCurrentBuildStatus: 'FAILURE',
                errorMessage: "Please check if the user can create report directory."
            )
        }

        Map generalConfiguration = script.commonPipelineEnvironment.configuration.general
        if (!generalConfiguration) {
            generalConfiguration = [:]
            script.commonPipelineEnvironment.configuration.general = generalConfiguration
        }

        def isMtaProject = fileExists('mta.yaml')
        def pomFile = 'pom.xml'
        if (isMtaProject) {
            setupMtaProject(script: script, generalConfiguration: generalConfiguration)
        } else if (fileExists(pomFile)) {
            pom = readMavenPom file: pomFile
            readAndUpdateProjectSalt(script, pomFile)
            Analytics.instance.hashProject(pom.groupId + pom.artifactId)
            if (!generalConfiguration.projectName?.trim()) {
                generalConfiguration.projectName = "${pom.groupId}-${pom.artifactId}"
            }
        } else {
            throw new Exception("No pom.xml or mta.yaml has been found in the root of the project. Currently the pipeline only supports Maven and Mta projects.")
        }

        if(!generalConfiguration.projectName){
            error "This should not happen: Project name was not specified in the configuration and could not be derived from the project."
        }

        ReportAggregator.instance.reportProjectIdentifier(generalConfiguration.projectName)

        if (env.JOB_URL) {
            Analytics.instance.hashBuildUrl(env.JOB_URL)
        } else {
            Analytics.instance.hashBuildUrl(env.JOB_NAME)
        }
        Analytics.instance.buildNumber(env.BUILD_NUMBER)

        Map configWithDefault = loadEffectiveGeneralConfiguration script: script

        if (isProductiveBranch(script: script) && configWithDefault.automaticVersioning) {
            artifactSetVersion script: script, buildTool: isMtaProject ? 'mta' : 'maven', filePath: isMtaProject ? 'mta.yaml' : 'pom.xml'
            ReportAggregator.instance.reportAutomaticVersioning()
        }
        generalConfiguration.gitCommitId = getGitCommitId()

        String prefix = generalConfiguration.projectName

        if (Boolean.valueOf(env.ON_K8S)) {
            initContainersMap script: script
        }

        script.commonPipelineEnvironment.configuration.currentBuildResultLock = "${prefix}/currentBuildResult"
        script.commonPipelineEnvironment.configuration.performanceTestLock = "${prefix}/performanceTest"
        script.commonPipelineEnvironment.configuration.endToEndTestLock = "${prefix}/endToEndTest"
        script.commonPipelineEnvironment.configuration.productionDeploymentLock = "${prefix}/productionDeployment"
        script.commonPipelineEnvironment.configuration.stashFiles = "${prefix}/stashFiles"

        initStageSkipConfiguration script: script
    }
}

private void readAndUpdateProjectSalt(script, pomFile) {
    try {
        def effectivePomFile = "effectivePom.xml"
        MavenUtils.generateEffectivePom(script, pomFile, effectivePomFile)
        if (fileExists(effectivePomFile)) {
            def projectSalt = getProjectSaltFromPom(readFile(effectivePomFile))
            if (projectSalt) {
                Analytics.instance.salt = projectSalt
            }
        }
    } catch (ignore) {

    }
}

@NonCPS
private String getProjectSaltFromPom(String pomfile) {
    String salt = null
    if (pomfile) {
        def plugins = new XmlSlurper().parseText(pomfile).depthFirst().findAll { it.name() == 'plugin' }
        for (def plugin : plugins) {
            if (plugin.artifactId == "s4sdk-maven-plugin") {
                salt = plugin.depthFirst().findAll {
                    it.name() == 'salt'
                }[0]
                return salt
            }
        }

    }
    return salt
}
