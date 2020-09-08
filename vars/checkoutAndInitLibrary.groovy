import com.sap.cloud.sdk.s4hana.pipeline.BuildTool
import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator

import com.sap.piper.DebugReport
import com.sap.piper.k8s.ContainerMap

def call(Map parameters) {
    def script = parameters.script

    Map scmCheckoutResult = checkout(parameters.checkoutMap ?: scm)

    initS4SdkPipelineLibrary script: script, configFile: parameters.configFile, customDefaults: parameters.customDefaults, customDefaultsFromFiles: parameters.customDefaultsFromFiles

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

    initAnalytics script: script

    initNpmModules script: script

    checkLegacyConfiguration script: script

    setupDownloadCache script: script

    checkMultibranchPipeline script: script

    if (!script.commonPipelineEnvironment.buildTool) {
        throw new Exception("No pom.xml, mta.yaml or package.json has been found in the root of the project. Currently the pipeline only supports Maven, Mta and JavaScript projects.")
    }
    BuildToolEnvironment.instance.setBuildTool(BuildTool.valueOf(script.commonPipelineEnvironment.buildTool.toUpperCase()))

    if (Boolean.valueOf(env.ON_K8S)) {
        String buildTool = BuildToolEnvironment.instance.getBuildTool().toString().toLowerCase()
        ContainerMap.instance.initFromResource(script, 'containers_map.yml', buildTool)
    }

    DebugReport.instance.buildTool = BuildToolEnvironment.instance.buildTool

    checkLegacyExtensions script: script

    if (!Boolean.valueOf(env.ON_K8S)) {
        checkDiskSpace script: script
    }
}
