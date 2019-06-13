import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.QualityCheck
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationLoader

import java.nio.file.Paths

def call(Map parameters = [:]) {
    def stageName = 'whitesourceScan'
    def script = parameters.script

    Map whitesourceConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)

    if(!whitesourceConfiguration) {
        error("Stage ${stageName} is not configured.")
    }

    runAsStage(stageName: stageName, script: script) {

        executeInRoot(script, whitesourceConfiguration)

        if (BuildToolEnvironment.instance.isMta()) {
            runOverModules(script: script, moduleType: "java") { basePath ->
                executeForMaven(script, basePath, whitesourceConfiguration)
            }
            runOverModules(script: script, moduleType: ["html5", "nodejs"]) { basePath ->
                executeForNpm(script, basePath, whitesourceConfiguration)
            }
        }
    }
}

private void executeInRoot(def script, Map whitesourceConfiguration) {
    String basePath = "."
    String packageJsonPath = Paths.get(basePath, "package.json").toString()

    boolean hasPackageJson = fileExists(packageJsonPath)

    if(BuildToolEnvironment.instance.isMaven()) {
        executeForMaven(script, basePath, whitesourceConfiguration)
    }

    if(hasPackageJson) {
        executeForNpm(script, basePath, whitesourceConfiguration)
    }
    else if(BuildToolEnvironment.instance.isNpm()){
        error("Folder '${basePath}' does not contain a package.json file. WhiteSource scan could not be performed.")
    }
}

private void executeForNpm(def script, String basePath, Map whitesourceConfiguration) {
    dir(basePath) {
        println("Executing WhiteSource scan for NPM module '${basePath}'")

        Map argumentMap = getWhiteSourceArgumentMap(script, whitesourceConfiguration)
        argumentMap['basePath'] = basePath

        executeWhitesourceScanNpm(argumentMap)

        ReportAggregator.instance.reportVulnerabilityScanExecution(QualityCheck.WhiteSourceScan)
    }
}

private void executeForMaven(def script, String basePath, Map whitesourceConfiguration) {
    println("Executing WhiteSource scan for Maven module '${basePath}'")

    Map argumentMap = getWhiteSourceArgumentMap(script, whitesourceConfiguration)
    argumentMap['pomPath'] = BuildToolEnvironment.instance.getApplicationPomXmlPath(basePath)

    executeWhitesourceScanMaven(argumentMap)

    ReportAggregator.instance.reportVulnerabilityScanExecution(QualityCheck.WhiteSourceScan)
}

private Map getWhiteSourceArgumentMap(script, Map whitesourceConfiguration) {
    Map whiteSourceArguments = [:]
    whiteSourceArguments['script'] = script
    whiteSourceArguments['credentialsId'] = whitesourceConfiguration.credentialsId
    whiteSourceArguments['product'] = whitesourceConfiguration.product

    if(whitesourceConfiguration.whitesourceUserTokenCredentialsId) {
        whiteSourceArguments['whitesourceUserTokenCredentialsId'] = whitesourceConfiguration.whitesourceUserTokenCredentialsId
    }

    return whiteSourceArguments
}
