def call(Map parameters = [:]) {
    def stageName = 'additionalTools'
    def script = parameters.script
    piperStageWrapper(stageName: stageName, script: script) {
        echo "No additional tools specified."
    }
}
