def call(Map parameters = [:]) {
    def stageName = 'postPipelineHook'
    def script = parameters.script
    piperStageWrapper(stageName: stageName, script: script) {
        // Stage does intentionally nothing as its purpose is to be overridden if required
    }
}
