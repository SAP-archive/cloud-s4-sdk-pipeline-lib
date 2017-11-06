import com.cloudbees.groovy.cps.NonCPS
import com.sap.icd.jenkins.ConfigurationLoader
import hudson.util.Secret

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'executeCheckmarxScan', stepParameters: parameters) {
        def script = parameters.script
        def checkmarxCredentialsId = parameters.get('checkmarxCredentialsId')
        def checkmarxGroupId = parameters.get('groupId')
        if (!checkmarxGroupId?.trim()) {
            currentBuild.result = 'FAILURE'
            throw new IllegalArgumentException("checkmarxGroupId value cannot be empty.")
        }

        def checkmarxProject = parameters.get('checkMarxProjectName', script.pipelineEnvironment.configuration.general.projectName)
        def checkmarxServerUrl = parameters.get('checkmarxServerUrl')
        def filterPattern = parameters.get('filterPattern')
        def fullScansScheduled = parameters.get('fullScansScheduled')
        def generatePdfReport = parameters.get('generatePdfReport')
        def incremental = parameters.get('incremental')
        def preset = parameters.get('preset')
        def vulnerabilityThresholdHigh = 0
        def vulnerabilityThresholdLow = parameters.get('vulnerabilityThresholdLow')
        def vulnerabilityThresholdMedium = parameters.get('vulnerabilityThresholdMedium')

        Map checkMarxOptions = [$class                       : 'CxScanBuilder',
                                avoidDuplicateProjectScans   : true,
                                filterPattern                : filterPattern,
                                fullScanCycle                : 10,
                                fullScansScheduled           : fullScansScheduled,
                                generatePdfReport            : generatePdfReport,
                                groupId                      : checkmarxGroupId,
                                highThreshold                : vulnerabilityThresholdHigh,
                                incremental                  : incremental,
                                lowThreshold                 : vulnerabilityThresholdLow,
                                mediumThreshold              : vulnerabilityThresholdMedium,
                                preset                       : preset,
                                projectName                  : checkmarxProject,
                                vulnerabilityThresholdEnabled: true,
                                vulnerabilityThresholdResult : 'FAILURE',
                                waitForResultsEnabled        : true]

        dir('application') {
            // Checkmarx scan
            if (checkmarxCredentialsId) withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: checkmarxCredentialsId, passwordVariable: 'password', usernameVariable: 'user']]) {
                if (checkmarxServerUrl?.trim()) {
                    checkMarxOptions.serverUrl = checkmarxServerUrl
                } else {
                    currentBuild.result = 'FAILURE'
                    throw new IllegalArgumentException("checkmarxServerUrl value cannot be empty while using checkmarxCredentialsId.")
                }
                checkMarxOptions.username = user
                checkMarxOptions.password = encryptPassword(password)
                checkMarxOptions.useOwnServerCredentials = true
                step(checkMarxOptions)
            } else step(checkMarxOptions)
        }
    }
}

@NonCPS
def encryptPassword(String password) {
    return Secret.fromString(password).getEncryptedValue()
}
