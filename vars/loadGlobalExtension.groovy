import com.sap.piper.DebugReport
import com.sap.piper.MapUtils

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'loadGlobalExtension', stepParameters: parameters) {
        def script = parameters.script

        String extensionRepository = getExtensionRepository(script, parameters.configFile)
        if (extensionRepository) {
            try {
                sh "git clone --depth 1 ${extensionRepository} ${s4SdkGlobals.repositoryExtensionsDirectory}"
                DebugReport.instance.globalExtensionRepository = extensionRepository
            } catch (Exception e) {
                error("Error while executing git clone for repository ${extensionRepository}.")
            }
        }
    }
}

private static String getExtensionRepository(Script script, String configFile) {
    try {
        Map projectConfig = script.readYaml file: configFile
        return projectConfig?.general?.extensionRepository
    } catch (Exception e) {
        script.echo "WARNING: Could not determine extensions repository from project config file " +
            "at '${configFile}'. Exception: ${e.getMessage()}"
    }
    return ''
}
