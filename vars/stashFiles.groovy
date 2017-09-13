def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'stashFiles', stepParameters: parameters) {
        def script = parameters.script
        def stage = parameters.stage

        Map stashConfig = script.pipelineEnvironment.configuration.s4SdkStashConfiguration

        for (def stash : stashConfig[stage].stashes) {
            def name = stash.name
            def include = stash.includes
            def exclude = stash.excludes


            if (stash?.merge == true) {
                String project = env.JOB_NAME.tokenize('/').first()
                String lockName = "${project}/stashFiles/${stash.name}"
                lock(lockName) {
                    unstash stash.name
                    steps.stash name: name, includes: include, exclude: exclude, allowEmpty: true
                }
            }
            else {
                steps.stash name: name, includes: include, exclude: exclude, allowEmpty: true
            }
        }
        deleteDir()
    }
}