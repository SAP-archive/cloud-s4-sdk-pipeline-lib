import com.cloudbees.groovy.cps.NonCPS
import com.sap.cloud.sdk.s4hana.pipeline.ProjectUtils
import com.sap.piper.ConfigurationLoader
import groovy.json.JsonSlurperClassic
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

        String projectName = ProjectUtils.getProjectName(script)

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
            jobStatusOnError             : 'FAILURE',
            // if this is set to true, the scan is not repeated for the same input even if the scan settings (e.g. thresholds) change
            avoidDuplicateProjectScans   : false,
            filterPattern                : filterPattern,
            fullScanCycle                : 5,
            fullScansScheduled           : fullScansScheduled,
            generatePdfReport            : generatePdfReport,
            groupId                      : checkmarxGroupId,
            highThreshold                : vulnerabilityThresholdHigh,
            incremental                  : incremental,
            lowThreshold                 : vulnerabilityThresholdLow,
            mediumThreshold              : vulnerabilityThresholdMedium,
            projectName                  : checkmarxProject,
            vulnerabilityThresholdEnabled: true,
            vulnerabilityThresholdResult : 'FAILURE',
            waitForResultsEnabled        : true
        ]

        if (checkmarxCredentialsId) {
            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: checkmarxCredentialsId, passwordVariable: 'password', usernameVariable: 'user']]) {
                if (checkmarxServerUrl?.trim()) {
                    checkMarxOptions.serverUrl = checkmarxServerUrl
                } else {
                    currentBuild.result = 'FAILURE'
                    throw new IllegalArgumentException("Value for checkmarxServerUrl cannot be empty while using checkmarxCredentialsId.")
                }
                checkMarxOptions.username = user
                checkMarxOptions.password = encryptPassword(password)
                checkMarxOptions.useOwnServerCredentials = true
            }
        }

        if (preset == null) {
            throw new RuntimeException("No preset configured. Checkmarx scan can only be executed with a given presetId or preset label.\n " +
                "For more information on preset, please refer to https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/configuration.md#checkmarxscan")
        }

        Integer presetId = null
        if (preset.isNumber()) {
            presetId = Integer.parseInt(preset)
        } else {
            if (checkMarxOptions.serverUrl && checkMarxOptions.username && checkMarxOptions.password) {
                    String token = getBearerToken(checkMarxOptions.serverUrl, checkMarxOptions.username, decryptPassword(checkMarxOptions.password))
                    presetId = getPresetIdFromCheckmarxserver(checkMarxOptions.serverUrl, token, preset)
            } else {
                throw new RuntimeException("When configuring the Checkmarx preset with a name, the attributes checkmarxCredentialsId and checkmarxServerUrl are mandatory."+
                    "For more information, please refer to https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/configuration.md#checkmarxscan")
            }
        }

        // Checkmarx-plugin expects a String
        checkMarxOptions.preset = presetId.toString()
        // Checkmarx scan
        step(checkMarxOptions)

        /*
        The checkmarx plugin sets the build result to UNSTABLE or FAILURE if the scan fails technically (e.g. connection error).
        In such a case, we actively fail the build. But if the scan was successful we don't fail the pipeline.
        */
        if (currentBuild.resultIsWorseOrEqualTo('UNSTABLE')) {
            // Execute on master - only here the log is accessible
            node('master') {
                String successString = "Checkmarx Scan Results(CxSAST)"
                String logFilePath = currentBuild.rawBuild.logFile.absolutePath
                boolean checkmarxExecuted =
                    (0 == sh(script: "grep --max-count 1 --fixed-strings -- '${successString}' ${logFilePath}", returnStatus: true))

                if (!checkmarxExecuted) {
                    currentBuild.result = 'FAILURE'
                    error "Aborting the build because Checkmarx scan did not execute successfully. Please have a look at the log output of the Checkmarx scan above."
                }
            }
        }
        // The checkmarx plugin does not abort the build when vulnerability thresholds are exceeded, therefore we actively fail the build
        if (currentBuild.resultIsWorseOrEqualTo('FAILURE')) {
            error "Aborting the build because the current build result is 'FAILURE'. Potentially the Checkmarx scan or a parallel stage did not execute successfully. Please have a look at the log output of the Checkmarx scan above."
        }
    }
}

@NonCPS
def decryptPassword(String password) {
    return Secret.decrypt(password).getPlainText()
}

@NonCPS
def encryptPassword(String password) {
    return Secret.fromString(password).getEncryptedValue()
}

@NonCPS
private String getBearerToken(String serverUrl, String username, String password) {
    String usernameEncoded = URLEncoder.encode(username, "UTF-8")
    String passwordEncoded = URLEncoder.encode(password, "UTF-8")

    String urlForm = "username=${usernameEncoded}&" +
                    "password=${passwordEncoded}&" +
                    "grant_type=password&" +
                    "scope=sast_rest_api&" +
                    "client_id=resource_owner_client&" +
                    // client_secret defined in Checkmarx API definition: https://checkmarx.atlassian.net/wiki/spaces/KC/pages/202506366/Token-based+Authentication+v8.6.0+and+up
                    "client_secret=014DF517-39D1-4453-B7B3-9930C563627C"

    String tokenJSON = doHttpPost(serverUrl + "/CxRestAPI/auth/identity/connect/token", urlForm, "application/x-www-form-urlencoded")
    Map jsonResponse = new JsonSlurperClassic().parseText(tokenJSON)

    if (jsonResponse.access_token) {
        return jsonResponse.access_token
    } else {
        throw new RuntimeException("""The answer from ${serverUrl} did not contain an access token.
                                       |Please verify that your Checkmarx server fulfills all prerequisites listed at
                                       |https://checkmarx.atlassian.net/wiki/spaces/KC/pages/202506366/Token-based+Authentication+v8.6.0+and+up""".stripMargin().stripIndent())
    }
}

@NonCPS
private int getPresetIdFromCheckmarxserver(String serverUrl, String token, String preset) {
    String presetResponse = doHttpGetWithToken(serverUrl + '/CxRestAPI/sast/presets', token)
    def presetsJSON = new JsonSlurperClassic().parseText(presetResponse)

    Map presets = [:]
    presetsJSON.each {
        presets.put(it.name, it.id)
    }

    Integer presetId = presets.get(preset)

    if (presetId == null) {
        throw new RuntimeException("""Could not get the presetId from Checkmarx server for given preset: ${preset}. Are you sure that this preset exists.
                                    |The following presets are available: ${presets}""".stripMargin().stripIndent())
    }
    return presetId
}

@NonCPS
private String doHttpGetWithToken(String serverUrl, String token) {
    int timeOutInMilliseconds = 5000
    HttpURLConnection connection = null
    String response = ''
    try {
        connection = (HttpURLConnection) new URL(serverUrl).openConnection()
        connection.setRequestMethod('GET')
        connection.setRequestProperty("Authorization", "Bearer ${token}")
        connection.setConnectTimeout(timeOutInMilliseconds)
        connection.setReadTimeout(timeOutInMilliseconds)
        connection.connect()

        int connectionReturncode = connection.getResponseCode()
        if (connectionReturncode >= 400) {
            throw new RuntimeException("Request of checkmarx preset list wasn't successul. HttpResponseCode was ${connectionReturncode}.")
        }

        response = connection.getInputStream().getText()
    } catch (IOException ioException) {
        throw new RuntimeException("Request of checkmarx preset list wasn't successul. Could not get the response: ${ioException.getMessage()}")
    } finally {
        connection?.disconnect()
    }
    return response
}

@NonCPS
private String doHttpPost(String serverUrl, String message, String contentType) {
    int timeOutInMilliseconds = 5000
    HttpURLConnection connection = null
    String response = ''
    try {
        connection = (HttpURLConnection) new URL(serverUrl).openConnection()
        connection.setRequestMethod('POST')
        connection.setDoOutput(true)
        connection.setConnectTimeout(timeOutInMilliseconds)
        connection.setReadTimeout(timeOutInMilliseconds)
        if (contentType) {
            connection.setRequestProperty("Content-Type", contentType)
        }
        connection.getOutputStream().write(message.getBytes("UTF-8"))

        int connectionReturncode = connection.getResponseCode()
        if (connectionReturncode >= 400) {
            throw new RuntimeException("Request of checkmarx authentication wasn't successul. HttpResponseCode was ${connectionReturncode}.")
        }

        response = connection.getInputStream().getText()
    } catch(IOException ioException) {
        throw new RuntimeException("Request of checkmarx authentication wasn't successul. ${ioException.getMessage()}")
    } finally {
        connection?.disconnect()
    }
    return response
}
