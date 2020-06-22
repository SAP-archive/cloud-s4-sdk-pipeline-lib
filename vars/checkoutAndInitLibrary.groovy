import com.sap.cloud.sdk.s4hana.pipeline.BuildTool
import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator

import com.sap.piper.DebugReport

def call(Map parameters) {
    def script = parameters.script

    Map scmCheckoutResult = checkout(parameters.checkoutMap ?: scm)

    initS4SdkPipelineLibrary script: script, configFile: parameters.configFile, customDefaults: parameters.customDefaults

    if (scmCheckoutResult.GIT_COMMIT) {
        ReportAggregator.instance.reportVersionControlUsed('Git')
    }

    if (!script.commonPipelineEnvironment.configuration.general) {
        script.commonPipelineEnvironment.configuration.general = [:]
    }

    if (scmCheckoutResult.GIT_URL) {
        script.commonPipelineEnvironment.configuration.general.gitUrl = scmCheckoutResult.GIT_URL
    }

    DebugReport.instance.setGitRepoInfo(scmCheckoutResult)

    initAnalytics(script: script)

    loadAdditionalLibraries script: script

    checkLegacyConfiguration script: script

    setupDownloadCache script: script

    checkMultibranchPipeline script: script

    if (Boolean.valueOf(env.ON_K8S)) {
        initContainersMap script: script
    }

    boolean isMtaProject = fileExists('mta.yaml')
    def isMaven = fileExists('pom.xml')
    def isNpm = fileExists('package.json')

    if (isMtaProject) {
        BuildToolEnvironment.instance.setBuildTool(BuildTool.MTA)
    } else if (isMaven) {
        BuildToolEnvironment.instance.setBuildTool(BuildTool.MAVEN)
    } else if (isNpm) {
        BuildToolEnvironment.instance.setBuildTool(BuildTool.NPM)
    } else {
        throw new Exception("No pom.xml, mta.yaml or package.json has been found in the root of the project. Currently the pipeline only supports Maven, Mta and JavaScript projects.")
    }

    DebugReport.instance.buildTool = BuildToolEnvironment.instance.buildTool

    checkLegacyExtensions(script: script)

    if (!Boolean.valueOf(env.ON_K8S)) {
        checkDiskSpace script: script
    }
}
