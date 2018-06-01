def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'unstashFiles', stepParameters: parameters) {
        def script = parameters.script
        String stage = parameters.stage

        deleteDir()

        List toUnstash = script.commonPipelineEnvironment.configuration.s4SdkStashConfiguration?.get(stage)?.unstash ?: []

        echo "Unstashing ${toUnstash}"

        for (int x = 0; x < toUnstash.size(); x++) {
            unstash toUnstash.get(x)
        }
    }
}
