import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'lint'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {
        final Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)

        runOverModules(script: script, moduleType: "html5") { basePath ->
            checkUi5BestPractices(script: script, configuration: stageConfiguration, basePath: basePath)
        }
    }
}
