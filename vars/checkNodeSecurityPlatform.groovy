import com.sap.cloud.sdk.s4hana.pipeline.BashUtils
def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkNodeSecurityPlatform', stepParameters: parameters) {
        final script = parameters.script
        try {
            executeNpm(script: script, dockerImage: 'allthings/nsp', dockerOptions: "--entrypoint=''") {
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
