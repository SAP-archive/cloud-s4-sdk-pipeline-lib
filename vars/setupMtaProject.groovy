def call(Map parameters = [:]) {
    def script = parameters.script
    def generalConfiguration = parameters.generalConfiguration
    def mta = readYaml file: 'mta.yaml'

    HashMap<String, ArrayList<String>> modules = getMtaModules(mta.modules)

    modules.entrySet().stream().forEach { Map.Entry<String, ArrayList<String>> entry ->
        echo entry.getKey() + " has modules:  " + entry.getValue().join(" - ")
    }

    generalConfiguration.projectName = mta.ID
    script.commonPipelineEnvironment.configuration.isMta = true
    script.commonPipelineEnvironment.configuration.mta = modules
    script.commonPipelineEnvironment.configuration.artifactId = mta.ID
}

private HashMap<String, ArrayList<String>> getMtaModules(ArrayList mta) {

    if (mta == null || mta.isEmpty()) {
        throw new Exception("No modules found in mta.yaml")
    }

    HashMap<String, ArrayList<String>> modules = new HashMap<>();

    mta.each { module ->
        if (modules.containsKey(module.type)) {
            ArrayList<String> list = modules.get(module.type)
            list.add(module.path)
            modules.put(module.type, list)
        } else {
            modules.put(module.type, new Collections.SingletonList<String>(module.path))
        }
    }
    return modules
}
