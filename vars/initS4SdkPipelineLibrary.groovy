import com.sap.piper.DebugReport

def call(Map parameters) {
    handleStepErrors(stepName: 'initS4SdkPipelineLibrary', stepParameters: parameters) {
        def script = parameters.script
        if (!parameters.configFile) {
            parameters.configFile = getConfigLocation(script: script)
        }
        parameters.customDefaults = parameters.customDefaults ?: ['default_s4_pipeline_environment.yml']

        loadGlobalExtension(parameters)

        String extensionConfigurationFilePath = "${s4SdkGlobals.repositoryExtensionsDirectory}/extension_configuration.yml"
        if (fileExists(extensionConfigurationFilePath)) {
            DebugReport.instance.globalExtensionConfigurationFilePath = extensionConfigurationFilePath
            parameters.customDefaultsFromFiles = [ extensionConfigurationFilePath ]
        }

        setupCommonPipelineEnvironment(parameters)
    }
}
