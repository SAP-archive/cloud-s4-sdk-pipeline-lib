import com.sap.cloud.sdk.s4hana.pipeline.Analytics
import com.sap.cloud.sdk.s4hana.pipeline.BuildTool
import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.cloud.sdk.s4hana.pipeline.Debuglogger
import com.sap.cloud.sdk.s4hana.pipeline.EnvironmentUtils

def call(Map parameters) {
    def script = parameters.script

    Map scmCheckoutResult = checkout(parameters.checkoutMap ?: scm)

    initS4SdkPipelineLibrary script: script, customDefaults: parameters.customDefaults

    if (scmCheckoutResult.GIT_COMMIT) {
        ReportAggregator.instance.reportVersionControlUsed('Git')
    }

    if (scmCheckoutResult.GIT_URL) {
        if (!script.commonPipelineEnvironment.configuration.general) {
            script.commonPipelineEnvironment.configuration.general = [:]
        }
        script.commonPipelineEnvironment.configuration.general.gitUrl = scmCheckoutResult.GIT_URL
        Debuglogger.instance.github.put("URI", scmCheckoutResult.GIT_URL)
        if (scmCheckoutResult.GIT_LOCAL_BRANCH) {
            Debuglogger.instance.github.put("branch", scmCheckoutResult.GIT_LOCAL_BRANCH)
        } else {
            Debuglogger.instance.github.put("branch", scmCheckoutResult.GIT_BRANCH)
        }
    }

    Analytics.instance.initAnalytics(script)

    loadGlobalExtension script: script
    convertLegacyExtensions(script: script)

    if (Boolean.valueOf(env.ON_K8S)) {
        initContainersMap script: script
    }

    Map configWithDefault = loadEffectiveGeneralConfiguration script: script
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

    Debuglogger.instance.buildTool = BuildToolEnvironment.instance.buildTool

    //TODO activate automatic versioning for JS
    if (!BuildToolEnvironment.instance.isNpm() && isProductiveBranch(script: script) && configWithDefault.automaticVersioning) {
        artifactSetVersion script: script, buildTool: isMtaProject ? 'mta' : 'maven', filePath: isMtaProject ? 'mta.yaml' : 'pom.xml'
        ReportAggregator.instance.reportAutomaticVersioning()
    }

    stash allowEmpty: true, excludes: '', includes: '**', useDefaultExcludes: false, name: 'scm'
}
