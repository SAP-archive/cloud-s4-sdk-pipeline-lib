import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkNodeSecurityPlatform', stepParameters: parameters) {
        final script = parameters.script

        runOverModules(script: script, moduleType: "html5") { basePath ->
            executeNodeSecurityPlatformCommand(script, basePath)
        }
    }
}

private void executeNodeSecurityPlatformCommand(def script, String basePath = './') {
    String command = ""
    if (basePath != null & !basePath.isEmpty()) {
        command = "cd $basePath"
    }
    command += '''
                set -o pipefail
                nsp check 2>&1 | tee nsp.log
                '''.stripIndent()

    try {
        def dockerOptions = ["--entrypoint=''"]
        DownloadCacheUtils.appendDownloadCacheNetworkOption(script, dockerOptions)

        executeNpm(script: script, dockerImage: 'allthings/nsp', dockerOptions: dockerOptions) {
            sh command
        }
    } finally {
        archiveArtifacts artifacts: 'nsp.log', allowEmptyArchive: true
    }
}
