def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'initS4SdkPipeline', stepParameters: parameters) {
        def script = parameters.script

        def mavenLocalRepository = new File("${workspace}/${script.s4SdkGlobals.m2Directory}")
        def reportsDirectory =  new File("${workspace}/${script.s4SdkGlobals.reportsDirectory}")

        mavenLocalRepository.mkdirs()
        reportsDirectory.mkdirs()
        if(!fileExists(mavenLocalRepository.absolutePath) || !fileExists(reportsDirectory.absolutePath)){
            errorWhenCurrentBuildResultIsWorseOrEqualTo(script.currentBuild, 'FAILURE', "Build was ABORTED and marked as FAILURE, please check if the user can create report directory.")
        }

        setupPipelineEnvironment(parameters)

        // Stash config map
        Map s4SdkStashConfiguration = readYaml(text: libraryResource('stash_settings.yml'))
        echo "Stash config: ${s4SdkStashConfiguration}"
        script.pipelineEnvironment.configuration.s4SdkStashConfiguration = s4SdkStashConfiguration

        stashFiles script: script, stage:'init'
    }
}
