import com.sap.cloud.sdk.s4hana.pipeline.Analytics
import com.sap.cloud.sdk.s4hana.pipeline.BuildTool
import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.cloud.sdk.s4hana.pipeline.Debuglogger
import com.sap.cloud.sdk.s4hana.pipeline.EnvironmentUtils
import com.sap.piper.MapUtils

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

    if (script.commonPipelineEnvironment.configuration.general.sharedConfiguration) {
        def response = httpRequest(
            url: script.commonPipelineEnvironment.configuration.general.sharedConfiguration,
            validResponseCodes: '100:399,404' // Allow a more specific error message for 404 case
        )
        if (response.status == 404) {
            error "File path for shared configuration (${script.commonPipelineEnvironment.configuration.general.sharedConfiguration}) appears to be incorrect. " +
                "Server returned HTTP status code 404. " +
                "Please make sure that the path is correct and no authentication is required to retrieve the file."
        }

        Map sharedConfig

        try {
            sharedConfig = readYaml text: response.content
        } catch (Exception e) {
            error "Failed to parse shared configuration as YAML file. " +
                "Please make sure it is valid YAML, and that the response body only contains valid YAML. " +
                "If you use a file from a GitHub repository, make sure you've used the 'raw' link, for example https://my.github.local/raw/someorg/shared-config/master/backend-service.yml\n" +
                "File path: ${script.commonPipelineEnvironment.configuration.general.sharedConfiguration}\n" +
                "Response content: ${response.content}\n" +
                "Exeption message: ${e.getMessage()}\n" +
                "Exception stacktrace: ${Arrays.toString(e.getStackTrace())}"
        }

        // The second parameter takes precedence, so shared config can be overridden by the project config
        script.commonPipelineEnvironment.configuration = MapUtils.merge(sharedConfig, script.commonPipelineEnvironment.configuration)
        Debuglogger.instance.sharedConfigFilePath = script.commonPipelineEnvironment.configuration.general.sharedConfiguration
    }

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
