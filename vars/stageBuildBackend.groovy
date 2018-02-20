import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'buildBackend'
    def script = parameters.script

    runAsStage(stageName: stageName, script: script) {
        Map configuration = ConfigurationLoader.stageConfiguration(script, stageName)

        executeMaven(
            script: script,
            flags: '-U -B',
            m2Path: s4SdkGlobals.m2Directory,
            goals: 'clean install',
            defines:'-Dmaven.test.skip=true',
            dockerImage: configuration.dockerImage
        )
    }
}
