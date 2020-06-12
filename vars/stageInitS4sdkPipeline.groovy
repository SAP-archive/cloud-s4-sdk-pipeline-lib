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
        checkoutAndInitLibrary(script: script, customDefaults: parameters.customDefaults)

        setArtifactVersion(script: script)

        // Stash git folder to be used in sonar later
        stash allowEmpty: true, excludes: '', includes: '**/.git/**', useDefaultExcludes: false, name: 'git'

        stash allowEmpty: true, excludes: '', includes: '**', useDefaultExcludes: false, name: 'INIT'
        script.commonPipelineEnvironment.configuration.stageStashes = [ initS4sdkPipeline: [ unstash : ["INIT"]]]
    }

    piperStageWrapper(stageName: 'initS4sdkPipeline', script: script) {
        initS4sdkPipeline(script: script)
    }
}
