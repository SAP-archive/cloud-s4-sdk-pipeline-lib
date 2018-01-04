import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationLoader

def call(Map parameters = [:]) {
    def script = parameters.script

    runAsStage(stageName: 'buildBackend', script: script) {
        Map configuration = ConfigurationLoader.stageConfiguration(script, 'buildBackend')

        unstashFiles script: script, stage:'buildBackend'

        executeMaven script: script, flags: '-U -B', m2Path: s4SdkGlobals.m2Directory, goals: 'clean install', defines:'-Dmaven.test.skip=true', dockerImage: configuration.dockerImage

        stashFiles script: script, stage:'buildBackend'
    }
}