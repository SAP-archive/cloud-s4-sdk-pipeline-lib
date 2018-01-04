import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationLoader

def call(Map parameters = [:]) {
    def script = parameters.script
    runAsStage(stageName: 'buildFrontend', script: script) {
        Map configuration = ConfigurationLoader.stageConfiguration(script, 'buildFrontend')

        unstashFiles script: script, stage: 'buildFrontend'
        if(fileExists('package.json')) {
            executeNpm(script: script, dockerImage: configuration.dockerImage, dockerOptions:'--cap-add=SYS_ADMIN') { sh "npm install" }
        }
        else {
            echo "Build Frontend skipped, because package.json does not exist!"
        }
        stashFiles script: script, stage: 'buildFrontend'
    }
}