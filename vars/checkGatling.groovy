import com.sap.cloud.sdk.s4hana.pipeline.BashUtils

def call(Map parameters = [:]) {

    handleStepErrors(stepName: 'checkGatling', stepParameters: parameters) {
        def script = parameters.script
        def appUrls = parameters.get('appUrls')

        try {
            if (appUrls) {
                for (int i = 0; i < appUrls.size(); i++) {
                    def appUrl = appUrls.get(i)
                    executeTestsWithAppUrlAndCredentials(script, appUrl.url, appUrl.credentialsId)
                }
            } else {
                mavenExecute script: script, flags: '--update-snapshots --batch-mode', pomPath: 'performance-tests/pom.xml', m2Path: s4SdkGlobals.m2Directory, goals: 'test'
            }
        }
        finally {
            gatlingArchive()
        }
    }
}

def executeTestsWithAppUrlAndCredentials(script, url, credentialsId) {
    withCredentials([
        [$class: 'UsernamePasswordMultiBinding', credentialsId: credentialsId, passwordVariable: 'PERFORMANCE_TEST_PASSWORD', usernameVariable: 'PERFORMANCE_TEST_USERNAME']
    ]) {
        def defines = "-DappUrl=${BashUtils.escape(url)} -Dusername=$PERFORMANCE_TEST_USERNAME, -Dpassword=${BashUtils.escape(PERFORMANCE_TEST_PASSWORD)}"
        mavenExecute script: script, flags: '--update-snapshots --batch-mode', pomPath: 'performance-tests/pom.xml', m2Path: s4SdkGlobals.m2Directory, goals: 'test', defines: defines
    }
}
