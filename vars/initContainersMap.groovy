import com.cloudbees.groovy.cps.NonCPS
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

@NonCPS
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

@NonCPS
def getDockerImageNameForStep(script, stageName, stepName) {
    def stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)
    final Map stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, stepName)

    final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, stepName)

    Set stageConfigurationKeys = ['dockerImage']
    Set stepConfigurationKeys = ['dockerImage']

    Map configuration = ConfigurationMerger.merge(stageConfiguration, stageConfigurationKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)

    return configuration.dockerImage ?: ''
}
