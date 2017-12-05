import com.sap.icd.jenkins.ConfigurationLoader

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'executeStaticCodeChecks', stepParameters: parameters) {
        dir('application') {
            def script = parameters.script
            unstashFiles script: script, stage: 'staticCodeChecks'

            Map configuration = ConfigurationLoader.stageConfiguration(script, 'staticCodeChecks')

            checkPmd script: script, excludes: configuration.pmdExcludes
            checkFindbugs script: script, excludeFilterFile: configuration.findbugsExcludesFile

            stashFiles script: script, stage: 'staticCodeChecks'
            echo "currentBuild.result: ${script.currentBuild.result}"
        }
    }
}