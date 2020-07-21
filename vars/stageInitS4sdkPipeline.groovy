def call(Map parameters) {
    def script = parameters.script

    loadPiper(script: script)

    /*
    In order to avoid the trust issues between the build server and the git server in a distributed setup,
    the init stage always executes on the master node. The underlying assumption here is that the Jenkins
    server has an SSH key and it has been added to the git server. This is necessary if Jenkins has to push
    code changes to the git server.
    */

    node(parameters.nodeLabel?:'master') {
        deleteDir()
        // The checkout has to happen outside of initS4sdkPipeline, in order for it to be extensible.
        // (An extension to "initS4sdkPipeline" has to exist in the workspace before entering piperStageWrapper.)
        checkoutAndInitLibrary(script: script, configFile: parameters.configFile, customDefaults: parameters.customDefaults, customDefaultsFromFiles: parameters.customDefaultsFromFiles)

        stash allowEmpty: true, excludes: '', includes: '**', useDefaultExcludes: false, name: 'INIT'
        script.commonPipelineEnvironment.configuration.stageStashes = [ initS4sdkPipeline: [ unstash : ["INIT"]]]
    }

    piperStageWrapper(stageName: 'initS4sdkPipeline', script: script) {
        initS4sdkPipeline(script: script)
    }
}
