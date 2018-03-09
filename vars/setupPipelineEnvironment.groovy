
def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'setupPipelineEnvironment', stepParameters: parameters) {
        def script = parameters.script

        script.pipelineEnvironment.defaultConfiguration = readYaml (text: libraryResource('default_s4_pipeline_environment.yml'))

        String configFile = parameters.get('configFile') ?: 'pipeline_config.yml'

        echo "Loading pipeline configuration from '${configFile}'"

        try {
            script.pipelineEnvironment.configuration = readYaml(file: configFile)
        }
        catch(e) {
            throw new RuntimeException("Failed to load the pipeline configuration from location '${configFile}'.", e)
        }

    }
}
