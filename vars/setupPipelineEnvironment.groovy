def call(Map parameters) {
    //FIXME Step to be backwards compatible for other consumers (sdk, testing-libs)
    loadPiper script: parameters.script
    setupCommonPipelineEnvironment(parameters)
    loadS4sdkDefaultValues script: parameters.script
}
