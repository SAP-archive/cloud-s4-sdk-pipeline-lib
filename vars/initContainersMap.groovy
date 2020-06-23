import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
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
    final Map stageConfiguration = loadEffectiveStageConfiguration(script: script, stageName: stageName)
    final Map stepConfiguration = loadEffectiveStepConfiguration(script: script, stepName: stepName)

    Map configuration = ConfigurationMerger.merge(stageConfiguration, null, stepConfiguration)

    String dockerImage = configuration.dockerImage

    if(!dockerImage && stepName == "mtaBuild"){
        dockerImage = configuration[configuration.mtaBuildTool]?.dockerImage
    }

    if(!dockerImage && stepName == "artifactPrepareVersion"){
        dockerImage = configuration[BuildToolEnvironment.instance.getBuildTool().getPiperBuildTool()]?.dockerImage
    }

    return dockerImage ?: ''
}
