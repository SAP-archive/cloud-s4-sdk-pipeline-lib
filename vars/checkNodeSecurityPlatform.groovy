import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkNodeSecurityPlatform', stepParameters: parameters) {
        final script = parameters.script

        try {
            def dockerOptions = ["--entrypoint=''"]
            DownloadCacheUtils.appendDownloadCacheNetworkOption(script, dockerOptions)

            executeNpm(script: script, dockerImage: 'allthings/nsp', dockerOptions: dockerOptions) {
                sh '''
                set -o pipefail
                nsp check 2>&1 | tee nsp.log
                '''.stripIndent()
            }
        } finally {
            archiveArtifacts artifacts: 'nsp.log', allowEmptyArchive: true
        }
    }
}
