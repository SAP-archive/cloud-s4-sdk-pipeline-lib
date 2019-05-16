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

        executeForAllTools(script, ".", whitesourceConfiguration)

        if (BuildToolEnvironment.instance.isMta()) {
            runOverModules(script: script, moduleType: "java") { basePath ->
                executeForMaven(script, basePath, whitesourceConfiguration)
            }
            runOverModules(script: script, moduleType: "html5") { basePath ->
                executeForNpm(script, basePath, whitesourceConfiguration)
            }
        }
    }
}

private void executeForAllTools(def script, String basePath, Map whitesourceConfiguration) {
    String packageJsonPath = Paths.get(basePath, "package.json").toString()
    String pomXmlPath = Paths.get(basePath, "application", "pom.xml").toString()

    boolean hasPackageJson = fileExists(packageJsonPath)
    boolean hasPomXml = fileExists(pomXmlPath)

    if(hasPomXml) {
        executeForMaven(script, basePath, whitesourceConfiguration)
    }

    if(hasPackageJson) {
        executeForNpm(script, basePath, whitesourceConfiguration)
    }

    if(!hasPackageJson && !hasPomXml && !BuildToolEnvironment.instance.isMta()) {
        println("Folder '${basePath}' neither contains a pom.xml nor a package.json file. No WhiteSource scan performed.")
    }

}

private void executeForNpm(def script, String basePath, Map whitesourceConfiguration) {
    dir(basePath) {
        println("Executing WhiteSource scan for NPM module '${basePath}'")

        executeWhitesourceScanNpm(getWhiteSourceArgumentMap(script, basePath, whitesourceConfiguration))

        ReportAggregator.instance.reportVulnerabilityScanExecution(QualityCheck.WhiteSourceScan)
    }
}

private void executeForMaven(def script, String basePath, Map whitesourceConfiguration) {
    println("Executing WhiteSource scan for Maven module '${basePath}'")

    executeWhitesourceScanMaven(getWhiteSourceArgumentMap(script, basePath, whitesourceConfiguration))

    ReportAggregator.instance.reportVulnerabilityScanExecution(QualityCheck.WhiteSourceScan)
}

private Map getWhiteSourceArgumentMap(script, String basePath, Map whitesourceConfiguration) {
    Map whiteSourceArguments = [:]
    whiteSourceArguments['script'] = script
    whiteSourceArguments['credentialsId'] = whitesourceConfiguration.credentialsId
    whiteSourceArguments['product'] = whitesourceConfiguration.product
    whiteSourceArguments['basePath'] = basePath

    if(whitesourceConfiguration.whitesourceUserTokenCredentialsId) {
        whiteSourceArguments['whitesourceUserTokenCredentialsId'] = whitesourceConfiguration.whitesourceUserTokenCredentialsId
    }

    return whiteSourceArguments
}
