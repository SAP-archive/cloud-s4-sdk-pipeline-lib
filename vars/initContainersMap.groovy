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

@NonCPS
Map getContainersMap(script) {
    Map containers = [:]
    def stageToStepMapping = ['buildBackend'        : ['mavenExecute': 'mavenExecute'],
                              'buildFrontend'       : ['executeNpm': 'executeNpm'],
                              'staticCodeChecks'    : ['mavenExecute': 'mavenExecute'],
                              'unitTests'           : ['mavenExecute': 'mavenExecute'],
                              'integrationTests'    : ['mavenExecute': 'mavenExecute'],
                              'frontendUnitTests'   : ['executeNpm': 'executeNpm'],
                              'nodeSecurityPlatform': ['executeNpm': 'checkNodeSecurityPlatform'],
                              'endToEndTests'       : ['mavenExecute': 'mavenExecute', 'executeNpm': 'executeNpm', 'cloudFoundryDeploy': 'cloudFoundryDeploy', 'deployToNeoWithCli': 'deployToNeoWithCli'],
                              'performanceTests'    : ['mavenExecute': 'mavenExecute', 'checkJMeter': 'checkJMeter', 'cloudFoundryDeploy': 'cloudFoundryDeploy', 'deployToNeoWithCli': 'deployToNeoWithCli'],
                              's4SdkQualityChecks'  : ['mavenExecute': 'mavenExecute'],
                              'artifactDeployment'  : ['mavenExecute': 'mavenExecute'],
                              'whitesourceScan'     : ['mavenExecute': 'mavenExecute', 'executeNpm': 'executeNpm'],
                              'sourceClearScan'     : ['executeSourceClearScan': 'executeSourceClearScan'],
                              'productionDeployment': ['mavenExecute': 'mavenExecute', 'executeNpm': 'executeNpm', 'cloudFoundryDeploy': 'cloudFoundryDeploy', 'deployToNeoWithCli': 'deployToNeoWithCli']

    ]
    stageToStepMapping.each { stageName, stepsMap -> containers[stageName] = getContainerForStage(script, stageName, stepsMap) }
    return containers
}

@NonCPS
def getContainerForStage(script, stageName, Map stepsMap) {
    def containers = [:]
    stepsMap.each { containerName, stepName ->
        def imageName = getDockerImageNameForStep(script, stageName, stepName)
        if (imageName) {
            containers[imageName] = containerName.toString().toLowerCase()
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
