import com.sap.piper.DebugReport

def call(Map parameters) {
    handleStepErrors(stepName: 'initS4SdkPipelineLibrary', stepParameters: parameters) {
        def script = parameters.script
        if (!parameters.configFile) {
            parameters.configFile = getConfigLocation(script: script)
        }
        parameters.customDefaults = parameters.customDefaults ?: ['default_s4_pipeline_environment.yml']
        parameters.customDefaultsFromFiles = parameters.customDefaultsFromFiles ?: ['default_s4_pipeline_environment.yml']

        setupCommonPipelineEnvironment(parameters)
    }
}
