import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger
import com.sap.piper.k8s.ContainerMap

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'initContainersMap', stepParameters: parameters) {
        def script = parameters.script
        ContainerMap.instance.setMap(getContainersMap(script))
    }
}

Map getContainersMap(script) {
    Map containers = [:]
    def stageToStepMapping = readYaml(text: libraryResource('containers_map.yml')).containerMaps
    stageToStepMapping.each { stageName, stepsList -> containers[stageName] = getContainerForStage(script, stageName, stepsList) }
    return containers
}

def getContainerForStage(script, stageName, List stepsList) {
    def containers = [:]
    stepsList.each { stepName ->
        def imageName = getDockerImageNameForStep(script, stageName, stepName)
        if (imageName) {
            containers[imageName] = stepName.toString().toLowerCase()
        }
    }
    return containers
}

def getDockerImageNameForStep(script, stageName, stepName) {
    def stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)
    final Map stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, stepName)

    final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, stepName)

    Map configuration = ConfigurationMerger.merge(stageConfiguration, null, stepConfiguration, null, stepDefaults)

    String dockerImage = configuration.dockerImage

    if(!dockerImage && stepName == "mtaBuild"){
        dockerImage = configuration[configuration.mtaBuildTool]?.dockerImage
    }

    return dockerImage ?: ''
}
