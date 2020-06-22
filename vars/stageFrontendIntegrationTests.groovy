def call(Map parameters = [:]) {
    def script = parameters.script
    def stageName = 'frontendIntegrationTests'
    piperPipelineStageIntegration script: script, stageName: stageName, npmExecuteScripts: true
}
