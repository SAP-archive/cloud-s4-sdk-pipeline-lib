import com.cloudbees.groovy.cps.NonCPS
import com.sap.cloud.sdk.s4hana.pipeline.Analytics
import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.MavenUtils
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.DebugReport

def call(Map parameters) {
    def script = parameters.script

    DebugReport.instance.initFromEnvironment(env)

    validateConfigSchema script: script

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
        if (!generalConfiguration.projectName?.trim()) {
            generalConfiguration.projectName = "${pom.groupId}-${pom.artifactId}"
        }

    } else if (BuildToolEnvironment.instance.isNpm()) {
        Map packageJson = readJSON file: 'package.json'
        def projectName = packageJson.name
        generalConfiguration.projectName = projectName ?: ''
        Analytics.instance.hashProject(generalConfiguration.projectName)
    }

    if (!generalConfiguration.projectName) {
        error "This should not happen: Project name was not specified in the configuration and could not be derived from the project."
    }

    initStashConfiguration script: script

    ReportAggregator.instance.reportProjectIdentifier(generalConfiguration.projectName)
    DebugReport.instance.projectIdentifier = generalConfiguration.projectName

    script.commonPipelineEnvironment.gitCommitId = getGitCommitId()

    String prefix = generalConfiguration.projectName

    script.commonPipelineEnvironment.configuration.currentBuildResultLock = "${prefix}/currentBuildResult"
    script.commonPipelineEnvironment.configuration.performanceTestLock = "${prefix}/performanceTest"
    script.commonPipelineEnvironment.configuration.endToEndTestLock = "${prefix}/endToEndTest"
    script.commonPipelineEnvironment.configuration.productionDeploymentLock = "${prefix}/productionDeployment"
    script.commonPipelineEnvironment.configuration.stashFiles = "${prefix}/stashFiles/${env.BUILD_TAG}"

    initNpmModules()

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
