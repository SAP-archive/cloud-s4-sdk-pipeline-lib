import com.sap.icd.jenkins.ConfigurationLoader
import com.sap.icd.jenkins.ConfigurationMerger

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'executeStaticCodeChecks', stepParameters: parameters) {
        def script = parameters.script

        unstashFiles script: script, stage: 'staticCodeChecks'

        Map configuration = ConfigurationLoader.stageConfiguration(script, 'stageStaticCodeChecks')

        errorWhenCurrentBuildResultIsWorseOrEqualTo(script.currentBuild, 'FAILURE', "Build was ABORTED and marked as FAILURE, because currentBuild.currentResult is ${script.currentBuild.currentResult}.")

        checkPmd script: script, excludes: configuration.pmdExcludes
        checkFindbugs script:script, excludeFilterFile:configuration.findbugsExcludesFile

        echo "Current build result: ${script.currentBuild.currentResult}"
        errorWhenCurrentBuildResultIsWorseOrEqualTo(script.currentBuild, 'FAILURE', "Build was ABORTED and marked as FAILURE, please examine static code check results.")

        stashFiles script: script, stage: 'staticCodeChecks'
    }
}