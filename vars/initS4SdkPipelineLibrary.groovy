import com.sap.piper.StageNameProvider

def call(Map parameters) {
    handleStepErrors(stepName: 'initS4SdkPipelineLibrary', stepParameters: parameters) {

        StageNameProvider.instance.useTechnicalStageNames = true

        def script = parameters.script
        if (!parameters.configFile) {
            parameters.configFile = getConfigLocation(script: script)
        }
        parameters.customDefaults = parameters.customDefaults ?: ['default_s4_pipeline_environment.yml']

        setupCommonPipelineEnvironment(parameters)
    }
}
