import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:]) {
    final String stepName = 'executeSourceClearScan'
    handleStepErrors(stepName: stepName, stepParameters: parameters) {
        def script = parameters.script
        final Map stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, stepName)
        final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, stepName)
        Set parameterKeys = ['dockerImage']
        Set stepConfigurationKeys = ['dockerImage']
        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)

        // Merge project defined configuration with the pipeline-defined configuration.
        // In the pipeline-defined configuration, we take care that SourceClear does not re-compile the Maven project,
        // and uses our settings file and m2 directory.
        Map sourceClearConfig = parameters.projectDefinedConfig ?: [:]
        Map pipelineDefinedSourceClearConfig = [
            fail_on             : 'high',
            compile_first       : false,
            install_first       : false,
            custom_maven_command: assembleCustomMavenCommands(script)
        ]

        sourceClearConfig.putAll(pipelineDefinedSourceClearConfig)

        def dockerOptions = effectiveMavenConfiguration(script).dockerOptions ?: []

        // Assumption: This is run in the project-root folder
        withCredentials([string(credentialsId: parameters.credentialsId, variable: 'TOKEN')]) {
            dockerExecute(
                dockerImage: configuration.dockerImage,
                dockerOptions: dockerOptions,
                dockerEnvVars: [
                    'SRCCLR_API_TOKEN'   : TOKEN,
                    'SRCCLR_SCM_NAME'    : parameters.projectName,
                    'SRCCLR_SCM_URI'     : parameters.scmUri,
                    'SRCCLR_SCM_REF'     : parameters.scmBranch,
                    'SRCCLR_SCM_REF_TYPE': 'branch',
                    'SRCCLR_SCM_REV'     : parameters.commitId
                ]) {
                writeYaml file: 'srcclr.yml', data: sourceClearConfig
                echo "Effective SourceClear Agent configuration:"
                sh 'cat srcclr.yml'
                def sourceClearStatus = sh script: 'curl -sSL  https://download.sourceclear.com/ci.sh | bash -s -- scan --recursive', returnStatus: true
                // Status codes are documented here: https://www.sourceclear.com/docs/scan-directives/#agent
                if (sourceClearStatus != 0) {
                    error("SourceClear scan has severe findings. Please examine the the full report.")
                }
            }
        }
    }
}

private Map effectiveMavenConfiguration(script) {
    final Map mavenStepDefaults = ConfigurationLoader.defaultStepConfiguration(script, 'mavenExecute')
    final Map mavenStepConfiguration = ConfigurationLoader.stepConfiguration(script, 'mavenExecute')
    Set mavenStepConfigurationKeys = ['globalSettingsFile', 'projectSettingsFile', 'dockerOptions']
    return ConfigurationMerger.merge(mavenStepConfiguration, mavenStepConfigurationKeys, mavenStepDefaults)
}

private String assembleCustomMavenCommands(script) {
    Map mavenConfiguration = effectiveMavenConfiguration(script)
    List customMavenCommands = ["-Dmaven.repo.local=${s4SdkGlobals.m2Directory}"]

    def globalSettingsFile = mavenConfiguration.globalSettingsFile
    if (globalSettingsFile?.trim()) {
        if (globalSettingsFile.trim().startsWith("http")) {
            String settings = fetchUrl(globalSettingsFile)
            writeFile file: 'global-settings.xml', text: settings
            globalSettingsFile = "global-settings.xml"
        }
        customMavenCommands.add("--global-settings '${globalSettingsFile}'")
    }

    def projectSettingsFile = mavenConfiguration.projectSettingsFile
    if (projectSettingsFile?.trim()) {
        if (projectSettingsFile.trim().startsWith("http")) {
            String settings = fetchUrl(projectSettingsFile)
            writeFile file: 'settings.xml', text: settings
            projectSettingsFile = "settings.xml"
        }
        customMavenCommands.add("--settings '${projectSettingsFile}'")
    }
    return customMavenCommands.join(' ')
}
