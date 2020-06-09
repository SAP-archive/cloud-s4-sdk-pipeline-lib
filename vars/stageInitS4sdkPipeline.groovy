def call(Map parameters) {
    def script = parameters.script

    loadPiper(script: script)

    piperStageWrapper(stageName: 'initS4sdkPipeline', script: script) {
        initS4sdkPipeline(script: script)
    }
}
