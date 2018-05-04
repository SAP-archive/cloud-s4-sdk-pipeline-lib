import com.cloudbees.groovy.cps.NonCPS
import com.sap.piper.ConfigurationLoader
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

        String projectName = ConfigurationLoader.generalConfiguration(script).projectName

        def checkmarxProject = parameters.checkMarxProjectName ?: projectName
        def checkmarxServerUrl = parameters.checkmarxServerUrl
        def filterPattern = parameters.filterPattern
        def fullScansScheduled = parameters.fullScansScheduled
        def generatePdfReport = parameters.generatePdfReport
        def incremental = parameters.incremental
        def preset = parameters.preset
        def vulnerabilityThresholdHigh = 0
        def vulnerabilityThresholdLow = parameters.vulnerabilityThresholdLow
        def vulnerabilityThresholdMedium = parameters.vulnerabilityThresholdMedium

        Map checkMarxOptions = [
            $class                       : 'CxScanBuilder',
            // if this is set to true, the scan is not repeated for the same input even if the scan settings (e.g. thresholds) change
            avoidDuplicateProjectScans   : false,
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
            waitForResultsEnabled        : true
        ]

        // Checkmarx scan
        if (checkmarxCredentialsId) {
            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: checkmarxCredentialsId, passwordVariable: 'password', usernameVariable: 'user']]) {
                if (checkmarxServerUrl?.trim()) {
                    checkMarxOptions.serverUrl = checkmarxServerUrl
                } else {
                    currentBuild.result = 'FAILURE'
                    throw new IllegalArgumentException("checkmarxServerUrl value cannot be empty while using checkmarxCredentialsId.")
                }
                checkMarxOptions.username = user
                checkMarxOptions.password = encryptPassword(password)
                checkMarxOptions.useOwnServerCredentials = true
            }
        }

        step(checkMarxOptions)

        /*
        The checkmarx plugin sets the build result to UNSTABLE if the scan fails technically (e.g. connection error).
        In such a case, we actively fail the build.
        */
        if (currentBuild.result == 'UNSTABLE') {
            // Execute on master - only here the log is accessible
            node('master') {
                String successString = "---Checkmarx Scan Results(CxSAST)---"
                String logFilePath = currentBuild.rawBuild.logFile.absolutePath
                boolean checkmarxExecuted =
                    (0 == sh(script: "grep --max-count 1 --fixed-strings -- '${successString}' ${logFilePath}", returnStatus: true))

                if (!checkmarxExecuted) {
                    currentBuild.result = 'FAILURE'
                    error "Aborting the build because Checkmarx scan did not execute successfully."
                }
            }
        }
    }
}

@NonCPS
def encryptPassword(String password) {
    return Secret.fromString(password).getEncryptedValue()
}
