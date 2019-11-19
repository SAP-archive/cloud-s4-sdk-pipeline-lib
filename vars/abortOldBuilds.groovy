import com.cloudbees.groovy.cps.NonCPS
import jenkins.model.*

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'abortOldBuilds', stepParameters: parameters) {
        def script = parameters.script;

        String jobName = script.JOB_NAME
        int currentBuildNumber = script.currentBuild.number

        if (!isProductiveBranch(script: script)) {
            abortOlderBuilds(jobName, currentBuildNumber)
        }
    }
}

@NonCPS
def abortOlderBuilds(String jobName, int currentBuildNumber) {
    def builds = Jenkins.instance.getItemByFullName(jobName).builds

    for (def build : builds) {
        if (build.isBuilding() && build.number.toInteger() < currentBuildNumber) {
            echo "The pipeline build ${build.number.toInteger()} is still executing. Aborting the build ${build.number.toInteger()}"
            build.doStop()
        }
    }
}

