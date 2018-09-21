def call(Map parameters) {
    //FIXME Step to be backwards compatible for other consumers (sdk, testing-libs)
    loadPiper script: parameters.script
    parameters.customDefaults = ['default_s4_pipeline_environment.yml']
    setupCommonPipelineEnvironment(parameters)
}
