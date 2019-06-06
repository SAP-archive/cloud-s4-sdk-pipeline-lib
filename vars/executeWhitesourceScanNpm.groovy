import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils
import net.sf.json.JSONObject

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'executeWhitesourceScanNpm', stepParameters: parameters) {
        final script = parameters.script

        withCredentials([string(credentialsId: parameters.credentialsId, variable: 'orgToken')]) {
            Map whiteSourceConfiguration = [
                apiKey       : orgToken,
                productName  : parameters.product,
                devDep       : false,
                checkPolicies: true
            ]

            if (parameters.projectName) {
                whiteSourceConfiguration.projectName = parameters.projectName
            }

            if (parameters.whitesourceUserTokenCredentialsId) {
                withCredentials([string(credentialsId: parameters.whitesourceUserTokenCredentialsId, variable: 'userKey')]) {
                    whiteSourceConfiguration.userKey = userKey
                }
            }

            if (fileExists('whitesource.config.json')) {
                error(
                    "File whitesource.config.json already exists. Please delete it and only use the file pipeline_config.yml to configure WhiteSource.\n" +
                    "Check https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/configuration.md#whitesourcescan for more details."
                )
            }

            writeJSON json: JSONObject.fromObject(whiteSourceConfiguration), file: "whitesource.config.json"


            try {
                executeNpm(script: script, dockerOptions: DownloadCacheUtils.downloadCacheNetworkParam()) {
                    sh 'npx whitesource run'
                }
            }
            finally {
                sh "rm -f whitesource.config.json"
            }
        }
    }
}
