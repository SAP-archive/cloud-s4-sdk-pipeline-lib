def call(Map parameters = [:]) {
    def script = parameters.script
    def stageName = 'backendIntegrationTests'
    piperPipelineStageIntegration script: script, stageName: stageName
}
