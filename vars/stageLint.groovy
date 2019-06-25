import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'lint'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {
        final Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)

        checkUi5BestPractices(script: script, configuration: stageConfiguration)
    }
}
