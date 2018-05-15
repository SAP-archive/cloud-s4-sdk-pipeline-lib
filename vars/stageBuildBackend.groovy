import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'buildBackend'
    def script = parameters.script

    runAsStage(stageName: stageName, script: script) {
        Map configuration = ConfigurationLoader.stageConfiguration(script, stageName)

        mavenExecute(
            script: script,
            flags: '--update-snapshots --batch-mode',
            m2Path: s4SdkGlobals.m2Directory,
            goals: 'clean install',
            defines: '-Dmaven.test.skip=true',
            dockerImage: configuration.dockerImage
        )
    }
}
