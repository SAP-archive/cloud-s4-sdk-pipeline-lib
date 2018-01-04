import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationLoader

def call(Map parameters = [:]) {
    def script = parameters.script
    runAsStage(stageName: 'staticCodeChecks', script: script) {
        dir('application') {
            unstashFiles script: script, stage: 'staticCodeChecks'

            Map configuration = ConfigurationLoader.stageConfiguration(script, 'staticCodeChecks')

            checkPmd script: script, excludes: configuration.pmdExcludes
            checkFindbugs script: script, excludeFilterFile: configuration.findbugsExcludesFile

            stashFiles script: script, stage: 'staticCodeChecks'
            echo "currentBuild.result: ${script.currentBuild.result}"
        }
    }
}