import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'fortifyScan'
    def script = parameters.script
    def stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)
    runAsStage(stageName: stageName, script: script) {
        executeFortifyScan(
            script: script,
            fortifyCredentialId: stageConfiguration.fortifyCredentialId,
            fortifyProjectName: stageConfiguration.fortifyProjectName,
            projectVersionId: stageConfiguration.projectVersionId,
            sscUrl: stageConfiguration.sscUrl
        )
    }
}

