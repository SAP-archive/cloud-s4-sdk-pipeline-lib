def call(Map parameters = [:]) {
    def stageName = 'additionalTools'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {
        echo "No additional tools specified."
    }
}
