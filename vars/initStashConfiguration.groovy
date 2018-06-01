def call(Map parameters) {
    def script = parameters.script

    Map s4SdkStashConfiguration = readYaml(text: libraryResource('stash_settings.yml'))
    echo "Stash config: ${s4SdkStashConfiguration}"
    script.commonPipelineEnvironment.configuration.s4SdkStashConfiguration = s4SdkStashConfiguration
}
