import com.sap.piper.MapUtils

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'loadGlobalExtension') {
        def script = parameters.script;

        String extensionRepository = loadEffectiveGeneralConfiguration(script: script).extensionRepository
        if (extensionRepository != null) {
            try {
                sh "git clone --depth 1 ${extensionRepository} ${s4SdkGlobals.repositoryExtensionsDirectory}"
            } catch (Exception e) {
                error("Error while executing git clone when accessing repository ${extensionRepository}.")
            }

            String extensionConfigurationFilePath = "${s4SdkGlobals.repositoryExtensionsDirectory}/extension_configuration.yml"
            if(fileExists(extensionConfigurationFilePath)){
                Map currentConfiguration = script.commonPipelineEnvironment.configuration
                Map extensionConfiguration = readYaml file:extensionConfigurationFilePath
                Map mergedConfiguration = MapUtils.merge(extensionConfiguration, currentConfiguration)
                script.commonPipelineEnvironment.configuration = mergedConfiguration
            }
        }
        loadAdditionalLibraries script: script
    }
}
