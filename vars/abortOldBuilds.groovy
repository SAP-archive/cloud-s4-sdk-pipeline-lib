import com.cloudbees.groovy.cps.NonCPS
import jenkins.model.*

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'abortOldBuilds') {
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
        if (build.number.toInteger() < currentBuildNumber) {
            build.doStop()
        }
    }
}

