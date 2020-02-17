import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils
import com.sap.cloud.sdk.s4hana.pipeline.QualityCheck
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'whitesourceScan'
    def script = parameters.script

    Map whitesourceConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)

    if (!whitesourceConfiguration) {
        error("Stage ${stageName} is not configured.")
    }

    piperStageWrapper(stageName: stageName, script: script) {

        if (fileExists('pom.xml')) {
            Map argumentMap = getWhiteSourceArgumentMap(script, whitesourceConfiguration)
            executeWhitesourceScanMaven(argumentMap)
        }
        runOverNpmModules(script: script) { String basePath ->
            executeForNpm(script, basePath, whitesourceConfiguration)
        }
        ReportAggregator.instance.reportVulnerabilityScanExecution(QualityCheck.WhiteSourceScan)
    }
}

private void executeForNpm(def script, String basePath, Map whitesourceConfiguration) {
    dir(basePath) {

        println("Executing WhiteSource scan for NPM module '${basePath}'")

        if (!fileExists('package-lock.json')) {
            echo "Expected npm package lock file to exist. This is a requirement for whitesource scans. Executing `npm install` to create a package-lock.json at '$basePath'."

            def dockerOptions = ['--cap-add=SYS_ADMIN']
            DownloadCacheUtils.appendDownloadCacheNetworkOption(script, dockerOptions)

            executeNpm(script: script, dockerOptions: dockerOptions) {
                sh 'npm install'
            }
        }

        Map argumentMap = getWhiteSourceArgumentMap(script, whitesourceConfiguration)

        executeWhitesourceScanNpm(argumentMap)
    }
}

private Map getWhiteSourceArgumentMap(script, Map whitesourceConfiguration) {
    Map whiteSourceArguments = [:]
    whiteSourceArguments['script'] = script
    whiteSourceArguments['credentialsId'] = whitesourceConfiguration.credentialsId
    whiteSourceArguments['product'] = whitesourceConfiguration.product
    whiteSourceArguments['productVersion'] = whitesourceConfiguration.staticVersion

    if (whitesourceConfiguration.whitesourceUserTokenCredentialsId) {
        whiteSourceArguments['whitesourceUserTokenCredentialsId'] = whitesourceConfiguration.whitesourceUserTokenCredentialsId
    }

    return whiteSourceArguments
}
