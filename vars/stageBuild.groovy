import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment

def call(Map parameters = [:]) {
    Script script = parameters.script
    build(script)

    if(BuildToolEnvironment.instance.isNpm()){
        packageJsApp(script)
    }
}

private build(Script script){
    def stageName = 'build'
    runAsStage(stageName: stageName, script: script) {
        if (BuildToolEnvironment.instance.isMta()) {

            withEnv(['MAVEN_OPTS=-Dmaven.repo.local=../s4hana_pipeline/maven_local_repo']) {
                mtaBuild(script: script)
            }
            // mta-builder executes 'npm install --production', therefore we need 'npm ci/install' to install the dev-dependencies
            runOverModules(script: script, moduleType: 'html5') { basePath ->
                dir(basePath){
                    installAndBuildNpm(script: script)
                }
            }
        } else if (BuildToolEnvironment.instance.isNpm()) {
            installAndBuildNpm script: script, customScripts:['ci-build']
        } else {
            mavenCleanInstall script: script
            if (fileExists('package.json')) {
                installAndBuildNpm script:script
            }
        }
    }
}

private packageJsApp(script){
    String stageName = 'package'
    runAsStage(stageName: stageName, script: script) {
        executeNpm(script: script, dockerOptions: []) {
            sh "npm run ci-package"
        }
    }
}
