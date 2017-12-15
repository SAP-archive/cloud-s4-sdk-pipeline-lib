def call(Map parameters = [:]) {
    def script = parameters.script
    runAsStage(stageName: 'nodeSecurityPlatform', script: script) {
        try {
            unstashFiles script: script, stage: 'nodeSecurityPlatform'
            checkNodeSecurityPlatform script: script
        } finally {
            stashFiles script: script, stage: 'nodeSecurityPlatform'
        }
    }
}