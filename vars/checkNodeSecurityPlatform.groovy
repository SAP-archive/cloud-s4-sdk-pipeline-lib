def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkNodeSecurityPlatform', stepParameters: parameters) {
        final script = parameters.script
        try {
            executeNpm(script: script, dockerImage: 'allthings/nsp') {
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
