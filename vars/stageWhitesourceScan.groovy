import com.sap.icd.jenkins.ConfigurationHelper
import com.sap.icd.jenkins.ConfigurationLoader

def call(Map parameters = [:]) {
    def script = parameters.script
    runAsStage(stageName: 'whitesourceScan', script: script) {
        try {
            unstashFiles script: script, stage: 'whitesourceScan'

            // Maven
            Map whitesourceConfiguration = ConfigurationLoader.stageConfiguration(script, 'whitesourceScan')
            if (whitesourceConfiguration) {
                def whitesourceConfigurationHelper = new ConfigurationHelper(whitesourceConfiguration)
                def orgToken = whitesourceConfigurationHelper.getConfigProperty('orgToken')
                def product = whitesourceConfigurationHelper.getConfigProperty('product')

                def stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'executeMaven')
                def globalSettingsFile = new ConfigurationHelper(stepConfiguration).getConfigProperty('globalSettingsFile')

                executeWhitesourceScanMaven script: script, orgToken: orgToken, product: product, globalSettingsFile: globalSettingsFile
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
        } finally {
            stashFiles script: script, stage: 'whitesourceScan'
        }
    }
}