import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'buildFrontend'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {
        Map configuration = ConfigurationLoader.stageConfiguration(script, stageName)
        if(fileExists('package.json')) {
            executeNpm(script: script, dockerImage: configuration.dockerImage, dockerOptions:'--cap-add=SYS_ADMIN') { sh "npm install" }
        }
        else {
            echo "Build Frontend skipped, because package.json does not exist!"
        }

    }
}