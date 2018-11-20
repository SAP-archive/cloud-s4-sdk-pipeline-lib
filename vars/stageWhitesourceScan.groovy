import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'whitesourceScan'
    def script = parameters.script

    runAsStage(stageName: stageName, script: script) {

        executeWhitesourceScan(script, stageName, "./")

        if (script.commonPipelineEnvironment.configuration.isMta) {
            runOverModules(script: script, moduleType: "java" ) { basePath ->
                executeWhitesourceScan(script, stageName, basePath)
            }
            runOverModules(script: script, moduleType: "html5" ) { basePath ->
                executeWhitesourceScan(script, stageName, basePath)
            }
        }
    }
}

private void executeWhitesourceScan(def script, String stageName, String basePath = './') {

    String pomXmlPath = "${basePath}/application/pom.xml"
    String packageJsonPath = "${basePath}/package.json"

    // Maven
    Map whitesourceConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)
    if (whitesourceConfiguration && fileExists(pomXmlPath)) {
        def whiteSourceArguments = [:]
        whiteSourceArguments['script'] = script
        whiteSourceArguments['pomPath'] = pomXmlPath
        whiteSourceArguments['credentialsId'] = whitesourceConfiguration.credentialsId
        whiteSourceArguments['product'] = whitesourceConfiguration.product
        if(whitesourceConfiguration.whitesourceUserTokenCredentialsId) {
            whiteSourceArguments['whitesourceUserTokenCredentialsId'] = whitesourceConfiguration.whitesourceUserTokenCredentialsId
        }

        println("Executing Maven WhiteSource scan for module " + basePath)
        executeWhitesourceScanMaven whiteSourceArguments
    } else {
        println("Skip WhiteSource Maven scan because the stage 'whitesourceScan' is not configured.")
    }

    // npm
    if (whitesourceConfiguration && fileExists(packageJsonPath)) {
        def whiteSourceArguments = [:]
        whiteSourceArguments['script'] = script
        whiteSourceArguments['basePath'] = basePath
        whiteSourceArguments['credentialsId'] = whitesourceConfiguration.credentialsId
        whiteSourceArguments['product'] = whitesourceConfiguration.product
        if(whitesourceConfiguration.whitesourceUserTokenCredentialsId) {
            whiteSourceArguments['whitesourceUserTokenCredentialsId'] = whitesourceConfiguration.whitesourceUserTokenCredentialsId
        }

        println("Executing npm WhiteSource scan for module " + basePath)

        executeWhitesourceScanNpm whiteSourceArguments
    } else {
        println("Skipping WhiteSource npm plugin because no 'package.json' file was found in project or the stage 'whitesourceScan' is not configured")
    }
}
