import com.sap.piper.MapUtils
import com.sap.piper.DebugReport

def call(Map parameters) {
    Script script = parameters.script
    if (!script.commonPipelineEnvironment.configuration.general?.sharedConfiguration) {
        return
    }

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
    DebugReport.instance.sharedConfigFilePath = script.commonPipelineEnvironment.configuration.general.sharedConfiguration
}
