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
        def credentialsId = whitesourceConfiguration.credentialsId
        def product = whitesourceConfiguration.product

        print("Executing MAVEN Whitesource scan for module " + basePath)
        executeWhitesourceScanMaven script: script, credentialsId: credentialsId, product: product, pomPath: pomXmlPath
    } else {
        println('Skip WhiteSource Maven scan because the stage "whitesourceScan" is not configured.')
    }

    // NPM
    if (whitesourceConfiguration && fileExists(packageJsonPath)) {
        def credentialsId = whitesourceConfiguration.credentialsId
        def product = whitesourceConfiguration.product

        print("Executing NPM Whitesource scan for module " + basePath)

        executeWhitesourceScanNpm(
            script: script,
            credentialsId: credentialsId,
            product: product,
            basePath: basePath
        )
    } else {
        println 'Skipping WhiteSource NPM Plugin because no "package.json" file was found in project or the stage "whitesourceScan" is not configured.\n'
    }
}
