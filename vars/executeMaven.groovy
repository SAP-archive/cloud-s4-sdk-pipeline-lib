import com.sap.cloud.sdk.s4hana.pipeline.BashUtils
import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationLoader
import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationMerger

def call(Map parameters = [:]) {

    handleStepErrors(stepName: 'executeMaven', stepParameters: parameters) {
        final script = parameters.script

        final Map stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, 'executeMaven')
        final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'executeMaven')

        List parameterKeys = [
            'dockerImage',
            'dockerOptions',
            'globalSettingsFile',
            'projectSettingsFile',
            'pomPath',
            'flags',
            'goals',
            'm2Path',
            'defines'
        ]
        List stepConfigurationKeys = [
            'dockerImage',
            'projectSettingsFile',
            'pomPath',
            'm2Path'
        ]

        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)

        String command = "mvn"

        def globalSettingsFile = configuration.globalSettingsFile
        if (globalSettingsFile?.trim()) {
            if(globalSettingsFile.trim().startsWith("http")){
                downloadSettingsFromUrl(globalSettingsFile)
                globalSettingsFile = "settings.xml"
            }
            command += " --global-settings ${BashUtils.escape(globalSettingsFile)}"
        }

        def m2Path = configuration.m2Path
        if(m2Path?.trim()) {
            command += " -Dmaven.repo.local=${BashUtils.escape(m2Path)}"
        }

        def projectSettingsFile = configuration.projectSettingsFile
        if (projectSettingsFile?.trim()) {
            if(projectSettingsFile.trim().startsWith("http")){
                downloadSettingsFromUrl(projectSettingsFile)
                projectSettingsFile = "settings.xml"
            }
            command += " --settings ${BashUtils.escape(projectSettingsFile)}"
        }

        def pomPath = configuration.pomPath
        if(pomPath?.trim()){
            command += " --file ${BashUtils.escape(pomPath)}"
        }

        def mavenFlags = configuration.flags
        if (mavenFlags?.trim()) {
            command += " ${mavenFlags}"
        }

        def mavenGoals = configuration.goals
        if (mavenGoals?.trim()) {
            command += " ${mavenGoals}"
        }
        def defines = configuration.defines
        if (defines?.trim()){
            command += " ${defines}"
        }

        executeDockerNative(dockerImage: configuration.dockerImage, dockerOptions: configuration.dockerOptions) {
            sh command
        }
    }
}

private downloadSettingsFromUrl(String url){
    String settings = fetchUrl(url)
    writeFile file: 'settings.xml', text: settings
}

