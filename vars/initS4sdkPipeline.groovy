import com.cloudbees.groovy.cps.NonCPS
import com.sap.cloud.sdk.s4hana.pipeline.Analytics
import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.MavenUtils
import com.sap.cloud.sdk.s4hana.pipeline.ProjectUtils
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.DebugReport

def call(Map parameters) {
    def script = parameters.script

    DebugReport.instance.initFromEnvironment(env)

    validateConfigSchema script: script

    setArtifactVersion(script: script)
    // Stash git folder to be used in sonar later
    stash allowEmpty: true, excludes: '', includes: '**/.git/**', useDefaultExcludes: false, name: 'git'

    Map generalConfiguration = script.commonPipelineEnvironment.configuration.general
    if (!generalConfiguration) {
        generalConfiguration = [:]
        script.commonPipelineEnvironment.configuration.general = generalConfiguration
    }

    def pomFile = 'pom.xml'

    if (BuildToolEnvironment.instance.isMta()) {
        setupMtaProject(script: script, generalConfiguration: generalConfiguration)
    } else if (BuildToolEnvironment.instance.isMaven()) {
        pom = readMavenPom file: pomFile
        readAndUpdateProjectSalt(script, pomFile)
        Analytics.instance.hashProject(pom.groupId + pom.artifactId)
    } else if (BuildToolEnvironment.instance.isNpm()) {
        Map packageJson = readJSON file: 'package.json'
        Analytics.instance.hashProject(packageJson.name)
    }

    initStashConfiguration script: script

    String projectName = ProjectUtils.getProjectName(script)
    ReportAggregator.instance.reportProjectIdentifier(projectName)
    DebugReport.instance.projectIdentifier = projectName

    script.commonPipelineEnvironment.gitCommitId = getGitCommitId()

    script.commonPipelineEnvironment.projectName = projectName
    String prefix = projectName

    script.commonPipelineEnvironment.configuration.currentBuildResultLock = "${prefix}/currentBuildResult"
    script.commonPipelineEnvironment.configuration.performanceTestLock = "${prefix}/performanceTest"
    script.commonPipelineEnvironment.configuration.stashFiles = "${prefix}/stashFiles/${env.BUILD_TAG}"

    //todo load with customized default config for now until issue 8490 is resolved
    piperInitRunStageConfiguration script: script, stageConfigResource: 'com.sap.piper/pipeline/cloudSdkStageDefaults.yml'
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
