def call(Map parameters) {
    def script = parameters.script

    loadPiper(script: script)

    /*
     Consumers can configure an environment variable 'CHECKOUT_NODE_LABEL' in Jenkins, to force the checkout
     to happen on a node with a specific label. This may be necessary in order to avoid trust issues between
     the build server and the git server in a distributed setup, if the SSK key or HTTPS certificates are
     only installed on certain agents. The default behavior is not to enforce a specific node.
     */

    String checkoutNodeLabel = ''
    if (env.CHECKOUT_NODE_LABEL in CharSequence) {
        checkoutNodeLabel = env.CHECKOUT_NODE_LABEL
    }

    node(checkoutNodeLabel) {
        deleteDir()
        // The checkout has to happen outside of initS4sdkPipeline, in order for it to be extensible.
        // (An extension to "initS4sdkPipeline" has to exist in the workspace before entering piperStageWrapper.)
        checkoutAndInitLibrary(script: script, customDefaults: parameters.customDefaults)

        stash allowEmpty: true, excludes: '', includes: '**', useDefaultExcludes: false, name: 'INIT'
        script.commonPipelineEnvironment.configuration.stageStashes = [ initS4sdkPipeline: [ unstash : ["INIT"]]]
    }

    piperStageWrapper(stageName: 'initS4sdkPipeline', script: script) {
        initS4sdkPipeline(script: script)
    }
}
