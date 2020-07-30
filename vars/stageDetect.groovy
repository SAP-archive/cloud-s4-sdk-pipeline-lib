

void call(Map parameters = [:]) {
    def stageName = 'detect'
    def script = parameters.script
    piperStageWrapper(stageName: stageName, script: script) {
        detectExecuteScan script: this
    }
}
