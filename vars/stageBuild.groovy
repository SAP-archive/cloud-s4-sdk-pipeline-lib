import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils

def call(Map parameters = [:]) {
    Script script = parameters.script
    String stageName = 'build'

    piperStageWrapper(stageName: stageName, script: script) {
        if (BuildToolEnvironment.instance.isMta()) {
            buildAndTestMta(script)
        } else if (BuildToolEnvironment.instance.isNpm()) {
            buildAndTestNpm(script)
        } else {
            buildAndTestMaven(script)
        }
    }

    if (BuildToolEnvironment.instance.isNpm()) {
        packageJsApp(script)
    }
}

private void buildAndTestMta(Script script) {
    collectJavaUnitTestResults(script: script) {
        mtaBuild(script: script)
    }
    prepareMtaBuildResultForNextStages(script: script)
    executeJavascriptUnitTests(script: script)
}

private void buildAndTestNpm(Script script) {
    installAndBuildNpm script: script, customScripts: ['ci-build']
    executeJavascriptUnitTests(script: script)
}

private void buildAndTestMaven(Script script) {
    collectJavaUnitTestResults(script: script) {
        mavenBuild(
            script: script,
            m2Path: s4SdkGlobals.m2Directory,
        )
    }
    // in case node_modules exists we assume npm install was executed by maven clean install
    if (fileExists('package.json') && !fileExists('node_modules')) {
        installAndBuildNpm script: script
    }
}

private packageJsApp(script) {
    String stageName = 'package'
    def dockerOptions = []
    DownloadCacheUtils.appendDownloadCacheNetworkOption(script, dockerOptions)
    piperStageWrapper(stageName: stageName, script: script) {
        executeNpm(script: script, dockerOptions: dockerOptions) {
            sh "npm run ci-package"
        }
    }
}
