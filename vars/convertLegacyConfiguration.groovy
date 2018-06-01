import com.sap.piper.ConfigurationLoader

def call(Map parameters) {
    def script = parameters.script

    renameMavenStep(script)
    removeMavenGlobalSettings(script)
    whiteSourceLegacyCheck(script)
}

def renameMavenStep(script) {
    def stepsConfiguration = script.commonPipelineEnvironment.configuration.steps

    if (stepsConfiguration?.executeMaven) {

        if (stepsConfiguration.mavenExecute) {
            error("Your pipeline configuration may only contain the step configuration mavenExecute. " +
                "The key executeMaven should not be used anymore.")
        }

        stepsConfiguration.mavenExecute = stepsConfiguration.executeMaven

        echo "[WARNING]: The configuration key executeMaven in the steps configuration should not be used anymore. " +
            "Please use mavenExecute instead."
    }
}

def removeMavenGlobalSettings(script) {
    Map mavenConfiguration = ConfigurationLoader.stepConfiguration(script, 'mavenExecute')

    // Maven globalSettings obsolete since introduction of DL-Cache
    if (mavenConfiguration.globalSettingsFile) {
        // switch to project settings if not also defined by user
        String projectSettingsFile = mavenConfiguration.projectSettingsFile
        if (!projectSettingsFile) {
            println("[WARNING]: Your pipeline configuration contains the obsolete configuration parameter " +
                "'executeMaven.globalSettingsFile=${mavenConfiguration.globalSettingsFile}'. The S/4HANA Cloud " +
                "SDK Pipeline uses an own global settings file to inject its download proxy as maven repository mirror. " +
                "Since you did not specify a project settings file, your settings file will be used as " +
                "'executeMaven.projectSettingsFile'.")

            mavenConfiguration.projectSettingsFile = mavenConfiguration.globalSettingsFile
            mavenConfiguration.remove('globalSettingsFile')
        } else {
            currentBuild.result = 'FAILURE'
            error("Your pipeline configuration contains the obsolete configuration parameter " +
                "'executeMaven.globalSettingsFile=${mavenConfiguration.globalSettingsFile}' together with " +
                "'executeMaven.projectSettingsFile=${mavenConfiguration.projectSettingsFile}'. " +
                "The S/4HANA Cloud SDK Pipeline uses an own global settings file to inject its download proxy " +
                "as maven repository mirror. Please reduce your settings to one file and specify " +
                "it under 'executeMaven.globalSettingsFile'.")
        }
    }
}

def whiteSourceLegacyCheck(script) {
    Map stageConfig = ConfigurationLoader.stageConfiguration(script, 'whitesourceScan')
    if (stageConfig) {
        if (stageConfig?.orgToken) {
            error("Your pipeline configuration may not use 'orgtoken' in whiteSourceScan stage. "+
                "Store it as a 'Secret Text' in Jenkins and use the 'credentialsId' field.")
        }
        if (!stageConfig?.credentialsId) {
            error("Your pipeline is using 'whiteSourceScan' and has a mandatory parameter 'credentialsId' not configured.")
        }
    }
}
