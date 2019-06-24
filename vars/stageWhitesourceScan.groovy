import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
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

    runAsStage(stageName: stageName, script: script) {

        if (BuildToolEnvironment.instance.isMta() || BuildToolEnvironment.instance.isMaven()) {

            runOverModules(script: script, moduleType: "java") { basePath ->
                executeForMaven(script, basePath, whitesourceConfiguration)
            }
        }
        runOverNpmModules(script: script) { basePath ->
            executeForNpm(script, basePath, whitesourceConfiguration)
        }
    }
}

private void executeForMaven(def script, String basePath, Map whitesourceConfiguration) {
    println("Executing WhiteSource scan for Maven module '${basePath}'")

    Map argumentMap = getWhiteSourceArgumentMap(script, whitesourceConfiguration)
    argumentMap['pomPath'] = BuildToolEnvironment.instance.getApplicationPomXmlPath(basePath)

    executeWhitesourceScanMaven(argumentMap)

    ReportAggregator.instance.reportVulnerabilityScanExecution(QualityCheck.WhiteSourceScan)
}

private void executeForNpm(def script, String basePath, Map whitesourceConfiguration) {
    dir(basePath) {
        println("Executing WhiteSource scan for NPM module '${basePath}'")

        Map argumentMap = getWhiteSourceArgumentMap(script, whitesourceConfiguration)

        executeWhitesourceScanNpm(argumentMap)

        ReportAggregator.instance.reportVulnerabilityScanExecution(QualityCheck.WhiteSourceScan)
    }
}

private Map getWhiteSourceArgumentMap(script, Map whitesourceConfiguration) {
    Map whiteSourceArguments = [:]
    whiteSourceArguments['script'] = script
    whiteSourceArguments['credentialsId'] = whitesourceConfiguration.credentialsId
    whiteSourceArguments['product'] = whitesourceConfiguration.product

    if (whitesourceConfiguration.whitesourceUserTokenCredentialsId) {
        whiteSourceArguments['whitesourceUserTokenCredentialsId'] = whitesourceConfiguration.whitesourceUserTokenCredentialsId
    }

    return whiteSourceArguments
}
