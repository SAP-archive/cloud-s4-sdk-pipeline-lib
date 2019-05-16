import com.cloudbees.groovy.cps.NonCPS
import com.sap.cloud.sdk.s4hana.pipeline.Analytics
import com.sap.cloud.sdk.s4hana.pipeline.BuildTool
import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.MavenUtils
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.DefaultValueCache
import com.sap.piper.MapUtils

def call(Map parameters) {
    def script = parameters.script
    
    Map scmCheckoutResult = checkout(parameters.checkoutMap ?: scm)

    if(scmCheckoutResult.GIT_COMMIT){
        ReportAggregator.instance.reportVersionControlUsed('Git')
    }

    initS4SdkPipelineLibrary script: script

    Analytics.instance.initAnalytics(script)

    loadGlobalExtension script: script

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

        BuildToolEnvironment.instance.setBuildTool(BuildTool.MAVEN)

        pom = readMavenPom file: pomFile
        readAndUpdateProjectSalt(script, pomFile)
        Analytics.instance.hashProject(pom.groupId + pom.artifactId)
        if (!generalConfiguration.projectName?.trim()) {
            generalConfiguration.projectName = "${pom.groupId}-${pom.artifactId}"
        }

    } else if(fileExists('package.json')){
        BuildToolEnvironment.instance.setBuildTool(BuildTool.NPM)
        Map packageJson = readJSON file: 'package.json'
        def projectName = packageJson.name
        generalConfiguration.projectName = projectName ?: ''
    }else {
        throw new Exception("No pom.xml, mta.yaml or package.json has been found in the root of the project. Currently the pipeline only supports Maven, Mta and JavaScript projects.")
    }

    if(!generalConfiguration.projectName){
        error "This should not happen: Project name was not specified in the configuration and could not be derived from the project."
    }

    initStashConfiguration script: script

    ReportAggregator.instance.reportProjectIdentifier(generalConfiguration.projectName)

    if (env.JOB_URL) {
        Analytics.instance.hashBuildUrl(env.JOB_URL)
    } else {
        Analytics.instance.hashBuildUrl(env.JOB_NAME)
    }
    Analytics.instance.buildNumber(env.BUILD_NUMBER)

    Map configWithDefault = loadEffectiveGeneralConfiguration script: script
    // ToDo activate automatic versioning for JS
    if (!BuildToolEnvironment.instance.isNpm() && isProductiveBranch(script: script) && configWithDefault.automaticVersioning) {
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
