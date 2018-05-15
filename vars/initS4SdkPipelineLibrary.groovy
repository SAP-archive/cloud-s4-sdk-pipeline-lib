def call(Map parameters) {
    handleStepErrors(stepName: 'initS4SdkPipelineLibrary', stepParameters: parameters) {
        def script = parameters.script

        loadPiper script: script

        if (!parameters.configFile) {
            parameters.configFile = 'pipeline_config.yml'
        }

        setupCommonPipelineEnvironment(parameters)

        loadS4sdkDefaultValues script: script

        convertLegacyConfiguration script: script

        setupDownloadCache script: script
    }
}
