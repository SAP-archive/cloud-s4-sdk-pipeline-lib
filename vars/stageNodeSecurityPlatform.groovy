def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'stageNodeSecurityPlatform', stepParameters: parameters) {
        def script = parameters.script

        try {
            unstashFiles script: script, stage: 'nodeSecurityPlatform'
            checkNodeSecurityPlatform script: script
        } finally {
            stashFiles script: script, stage: 'nodeSecurityPlatform'
        }
    }
}