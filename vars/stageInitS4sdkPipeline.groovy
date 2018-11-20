import com.sap.cloud.sdk.s4hana.pipeline.Analytics

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
        checkout scm

        initS4SdkPipelineLibrary script: script
        initStashConfiguration script: script

        initSalt()

        Analytics.instance.initAnalytics(isProductiveBranch(script: script), script.commonPipelineEnvironment.configuration.general.idsite)

        String extensionRepository = script.commonPipelineEnvironment.configuration.general.extensionRepository
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

        if (env.'JOB_URL') {
            Analytics.instance.hashBuildUrl(env.'JOB_URL')
        } else {
            Analytics.instance.hashBuildUrl(env.'JOB_NAME')
        }

        Analytics.instance.hashBuildNumber(env.'BUILD_NUMBER')

        def isMtaProject = fileExists('mta.yaml')
        if (isMtaProject) {
            setupMtaProject(script: script, generalConfiguration: generalConfiguration)
        } else if (fileExists('pom.xml')) {
            pom = readMavenPom file: 'pom.xml'

            Analytics.instance.hashProject(pom.groupId + pom.artifactId, null) // todo read project specific salt from POM
            if (!generalConfiguration.projectName?.trim()) {
                generalConfiguration.projectName = pom.artifactId
            }
        } else {
            throw new Exception("No pom.xml or mta.yaml has been found in the root of the project. Currently the pipeline only supports Maven and Mta projects.")
        }

        Map configWithDefault = loadEffectiveGeneralConfiguration script: script

        if (isProductiveBranch(script: script) && configWithDefault.automaticVersioning) {
            artifactSetVersion script: script, buildTool: isMtaProject ? 'mta' : 'maven', filePath: isMtaProject ? 'mta.yaml' : 'pom.xml'
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

private void initSalt() {
    try {
        String salt
        if (!fileExists('/var/jenkins_home/s4sdk-salt')) {
            salt = generateSalt()
            sh "echo ${salt} > /var/jenkins_home/s4sdk-salt"
        } else {
            salt = sh script: 'cat /var/jenkins_home/s4sdk-salt', returnStdout: true
        }
        Analytics.instance.salt = salt
    } catch (Exception e) {
        // Proceed without salt, don't hash sensitive values
    }
}

private String generateSalt() {
    byte[] salt = new byte[20]
    Random random = new Random()
    random.nextBytes(salt)
    String saltHex = ''
    for (character in salt) {
        saltHex += String.format("%02x", character)
    }
    return saltHex
}
