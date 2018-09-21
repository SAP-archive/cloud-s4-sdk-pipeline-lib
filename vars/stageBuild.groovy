import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils

def call(Map parameters = [:]) {
    def stageName = 'build'
    def script = parameters.script

    runAsStage(stageName: stageName, script: script) {
        if (script.commonPipelineEnvironment.configuration.isMta) {
            withEnv(['MAVEN_OPTS=-Dmaven.repo.local=../s4hana_pipeline/maven_local_repo -Dmaven.test.skip=true', 'npm_config_package_lock=false']) {
                mtaBuild(script: script)
            }
        } else {
            mavenExecute(
                script: script,
                flags: '--update-snapshots --batch-mode',
                m2Path: s4SdkGlobals.m2Directory,
                goals: 'clean install',
                defines: '-Dmaven.test.skip=true',
            )
            if (fileExists('package.json')) {
                def dockerOptions = ['--cap-add=SYS_ADMIN']
                DownloadCacheUtils.appendDownloadCacheNetworkOption(script, dockerOptions)
                if (fileExists('package-lock.json') || fileExists('npm-shrinkwrap.json')) {
                    executeNpm(script: script, dockerOptions: dockerOptions) {
                        sh "npm ci"
                    }
                } else {
                    executeNpm(script: script, dockerOptions: dockerOptions) {
                        sh "npm install --package-lock false"
                    }
                }
            }
        }
    }
}
