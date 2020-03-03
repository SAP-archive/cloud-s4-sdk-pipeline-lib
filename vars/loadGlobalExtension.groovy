import com.sap.piper.DebugReport
import com.sap.piper.MapUtils

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'loadGlobalExtension', stepParameters: parameters) {
        def script = parameters.script

        String extensionRepository = loadEffectiveGeneralConfiguration(script: script).extensionRepository
        if (extensionRepository != null) {
            try {
                sh "git clone --depth 1 ${extensionRepository} ${s4SdkGlobals.repositoryExtensionsDirectory}"
                DebugReport.instance.globalExtensionRepository = extensionRepository
            } catch (Exception e) {
                error("Error while executing git clone for repository ${extensionRepository}.")
            }

            String extensionConfigurationFilePath = "${s4SdkGlobals.repositoryExtensionsDirectory}/extension_configuration.yml"
            if (fileExists(extensionConfigurationFilePath)) {
                DebugReport.instance.globalExtensionConfigurationFilePath = extensionConfigurationFilePath
                Map currentConfiguration = script.commonPipelineEnvironment.configuration
                Map extensionConfiguration = readYaml file: extensionConfigurationFilePath
                // The second parameter takes precedence, so extension config can be overridden by the project config
                Map mergedConfiguration = MapUtils.merge(extensionConfiguration, currentConfiguration)
                script.commonPipelineEnvironment.configuration = mergedConfiguration
            }
        }
        loadAdditionalLibraries script: script
    }
}
