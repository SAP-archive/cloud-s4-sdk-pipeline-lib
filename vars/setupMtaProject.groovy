import com.sap.cloud.sdk.s4hana.pipeline.Analytics
import com.sap.cloud.sdk.s4hana.pipeline.BuildTool
import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment

def call(Map parameters = [:]) {
    Script script = parameters.script
    Map generalConfiguration = parameters.generalConfiguration
    Map mta = readYaml file: 'mta.yaml'

    Map<String, List<String>> modules = getMtaModules(mta.modules)

    modules.entrySet().stream().forEach { Map.Entry<String, ArrayList<String>> entry ->
        echo entry.getKey() + " has modules:  " + entry.getValue().join(" - ")
    }

    generalConfiguration.projectName = mta.ID

    BuildToolEnvironment.instance.setBuildTool(BuildTool.MTA)
    BuildToolEnvironment.instance.setModules(modules)

    script.commonPipelineEnvironment.configuration.artifactId = mta.ID
    // TODO Need salt
    Analytics.instance.hashProject(mta.ID)
}

private Map<String, List<String>> getMtaModules(List mta) {

    if (mta == null || mta.isEmpty()) {
        throw new Exception("No modules found in mta.yaml")
    }

    Map<String, List<String>> modules = new HashMap<>();

    mta.each { module ->
        if (modules.containsKey(module.type)) {
            List<String> list = modules.get(module.type)
            list.add(module.path)
            modules.put(module.type, list)
        } else {
            modules.put(module.type, new Collections.SingletonList<String>(module.path))
        }
    }
    return modules
}
