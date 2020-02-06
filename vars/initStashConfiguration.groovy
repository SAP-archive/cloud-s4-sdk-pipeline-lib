import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment

def call(Map parameters) {
    def script = parameters.script

    String stashSettingsFileName
    if (BuildToolEnvironment.instance.isNpm()) {
        stashSettingsFileName = 'javascript_stash_settings.yml'
    } else if (BuildToolEnvironment.instance.isMta()) {
        stashSettingsFileName = 'cap_stash_settings.yml'
    } else {
        stashSettingsFileName = 'java_stash_settings.yml'
    }

    Map s4SdkStashConfiguration = readYaml(text: libraryResource(stashSettingsFileName))
    echo "Stash config: ${s4SdkStashConfiguration}"
    script.commonPipelineEnvironment.configuration.stageStashes = s4SdkStashConfiguration
}
