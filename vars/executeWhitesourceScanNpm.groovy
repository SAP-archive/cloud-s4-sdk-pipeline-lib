def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'executeWhitesourceScanNpm', stepParameters: parameters) {
        final script = parameters.script
        try {
            executeNpm(script: script) {
                sh """
                npm install whitesource
                alias whitesource='node node_modules/whitesource/bin/whitesource.js'
                whitesource run || echo 'whitesource scan failed'
                npm ls --json > whitesourceJsonTree.json || echo 'npm ls failed'
                """.trim()
            }
        } finally {
            archiveArtifacts artifacts: 'ws-l*', allowEmptyArchive: true
        }
    }
}