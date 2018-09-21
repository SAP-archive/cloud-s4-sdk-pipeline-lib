import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils
import net.sf.json.JSONObject

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'executeWhitesourceScanNpm', stepParameters: parameters) {
        final script = parameters.script
        String basePath = parameters.basePath

        withCredentials([string(credentialsId: parameters.credentialsId, variable: 'orgToken')]) {
            Map whiteSourceConfiguration = [
                apiKey       : orgToken,
                productName  : parameters.product,
                devDep       : false,
                checkPolicies: true,
            ]

            if (parameters.projectName) {
                whiteSourceConfiguration.projectName = parameters.projectName
            }

            if (fileExists('whitesource.config.json')) {
                error("File whitesource.config.json already exists. " +
                    "Please delete it and only use the file pipeline_config.yml to configure Whitesource.")
            }

            writeJSON json: JSONObject.fromObject(whiteSourceConfiguration), file: basePath + '/whitesource.config.json'

            try {
                executeNpm(script: script, dockerOptions: DownloadCacheUtils.downloadCacheNetworkParam()) {
                    dir(basePath){
                        sh 'npx whitesource run'
                    }
                }
            } finally {
                archiveArtifacts artifacts: 'ws-l*', allowEmptyArchive: true
            }
        }
    }
}
