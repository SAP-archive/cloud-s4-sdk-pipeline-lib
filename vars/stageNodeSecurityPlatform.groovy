def call(Map parameters = [:]) {
    def stageName = 'nodeSecurityPlatform'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {
        checkNodeSecurityPlatform script: script
    }
}