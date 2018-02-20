import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationHelper
import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'whitesourceScan'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {
        // Maven
        Map whitesourceConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)
        if (whitesourceConfiguration) {
            def whitesourceConfigurationHelper = new ConfigurationHelper(whitesourceConfiguration)
            def orgToken = whitesourceConfigurationHelper.getConfigProperty('orgToken')
            def product = whitesourceConfigurationHelper.getConfigProperty('product')

            executeWhitesourceScanMaven script: script, orgToken: orgToken, product: product
        } else {
            println('Skip WhiteSource Maven scan because the stage "whitesourceScan" is not configured.')
        }

        // NPM
        if (fileExists('whitesource.config.json')) {
            executeWhitesourceScanNpm script: script
        } else {
            println 'Skip WhiteSource NPM Plugin, because no "whitesource.config.json" file was found in project.\n' +
                    'Please refer to http://docs.whitesourcesoftware.com/display/serviceDocs/NPM+Plugin for usage information.'
        }
    }
}
