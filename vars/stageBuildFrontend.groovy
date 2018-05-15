import com.sap.piper.ConfigurationLoader
import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils

def call(Map parameters = [:]) {
    def stageName = 'buildFrontend'
    def script = parameters.script

    def dockerOptions = ['--cap-add=SYS_ADMIN']
    DownloadCacheUtils.appendDownloadCacheNetworkOption(script, dockerOptions)

    runAsStage(stageName: stageName, script: script) {
        Map configuration = ConfigurationLoader.stageConfiguration(script, stageName)

        if (fileExists('package.json')) {
            executeNpm(script: script, dockerImage: configuration.dockerImage, dockerOptions: dockerOptions) {
                sh "npm install"
            }
        } else {
            echo "Build Frontend skipped, because package.json does not exist!"
        }

    }
}
