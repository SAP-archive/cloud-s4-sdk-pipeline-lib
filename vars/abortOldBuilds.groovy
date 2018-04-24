import jenkins.model.*
import com.cloudbees.groovy.cps.NonCPS

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'abortOldBuilds') {
        def script = parameters.script;

        String jobName = script.JOB_NAME
        int currentBuildNumber = script.currentBuild.number

        if(script.env.BRANCH_NAME != script.commonPipelineEnvironment.configuration.general.productiveBranch) {
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

