def call(Map parameters = [:], body) {
    def script = parameters.script
    def moduleType = parameters.moduleType

    List<String> modules = []

    if (script.commonPipelineEnvironment.configuration.isMta) {
        Map<String, List<String>> mta = script.commonPipelineEnvironment.configuration.mta

        modules = mta.get(moduleType)

        if (modules == null || modules.isEmpty()) {
            return
        }
    } else {
        modules.add("./")
    }

    modules.each { basePath ->
        body(basePath)
    }

}
