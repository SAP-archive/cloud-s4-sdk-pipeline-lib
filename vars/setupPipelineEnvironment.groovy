
def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'setupPipelineEnvironment', stepParameters: parameters) {
        String defaultYmlConfigFile = 'pipeline_config.yml'
        String configFile = parameters.get('configFile', '')

        def script = parameters.script

        script.pipelineEnvironment.defaultConfiguration = readYaml (text: libraryResource('default_pipeline_environment.yml'))

        if(configFile.trim().length() == 0 && fileExists(defaultYmlConfigFile)) {
            configFile = defaultYmlConfigFile
        }

        echo "Loading configuration from ${configFile}"
        if (fileExists(configFile) && configFile.endsWith(".yml")) {
            script.pipelineEnvironment.configuration = readYaml(file: configFile)
        } else {
            throw new Exception("ERROR - CONFIG FILE MUST BE A .yml FILE")
        }
    }
}
