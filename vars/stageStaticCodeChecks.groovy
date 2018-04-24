import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'staticCodeChecks'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {
        Map configuration = ConfigurationLoader.stageConfiguration(script, stageName)

        checkPmd script: script, excludes: configuration.pmdExcludes
        checkFindbugs script: script, excludeFilterFile: configuration.findbugsExcludesFile
    }
}
