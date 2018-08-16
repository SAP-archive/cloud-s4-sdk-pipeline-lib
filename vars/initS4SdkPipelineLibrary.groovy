def call(Map parameters) {
    handleStepErrors(stepName: 'initS4SdkPipelineLibrary', stepParameters: parameters) {
        def script = parameters.script

        loadPiper script: script

        if (!parameters.configFile) {
            parameters.configFile = 'pipeline_config.yml'
        }

        parameters.customDefaults = ['default_s4_pipeline_environment.yml']

        setupCommonPipelineEnvironment(parameters)

        convertLegacyConfiguration script: script

        setupDownloadCache script: script
    }
}
