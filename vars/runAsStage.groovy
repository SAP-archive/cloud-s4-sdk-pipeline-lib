import com.sap.piper.ConfigurationHelper
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger
import com.sap.piper.k8s.ContainerMap

import java.util.UUID

def call(Map parameters = [:], body) {
    ConfigurationHelper configurationHelper = new ConfigurationHelper(parameters)
    def stageName = configurationHelper.getMandatoryProperty('stageName')
    def script = configurationHelper.getMandatoryProperty('script')
    Map defaultGeneralConfiguration = ConfigurationLoader.defaultGeneralConfiguration(script)
    Map projectGeneralConfiguration = ConfigurationLoader.generalConfiguration(script)

    Map generalConfiguration = ConfigurationMerger.merge(
        projectGeneralConfiguration,
        projectGeneralConfiguration.keySet(),
        defaultGeneralConfiguration
    )

    Map stageDefaultConfiguration = ConfigurationLoader.defaultStageConfiguration(script, stageName)
    Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)

    Set parameterKeys = ['node']
    Map mergedStageConfiguration = ConfigurationMerger.merge(
        parameters,
        parameterKeys,
        stageConfiguration,
        stageConfiguration.keySet(),
        stageDefaultConfiguration
    )
    mergedStageConfiguration.uniqueId = UUID.randomUUID().toString()
    String nodeLabel = generalConfiguration.defaultNode

    def containerMap = ContainerMap.instance.getMap().get(stageName) ?: [:]
    if (mergedStageConfiguration.node) {
        nodeLabel = mergedStageConfiguration.node
    }
    handleStepErrors(stepName: stageName, stepParameters: [:]) {
        if (Boolean.valueOf(env.ON_K8S) && containerMap.size() > 0) {
            withEnv(["POD_NAME=${stageName}"]) {
                dockerExecuteOnKubernetes(script: script, containerMap: containerMap) {
                        unstashFiles script: script, stage: stageName
                        executeStage(body, stageName, mergedStageConfiguration, generalConfiguration)
                        stashFiles script: script, stage: stageName
                        echo "Current build result in stage $stageName is ${script.currentBuild.result}."
                }
            }
        } else {
            node(nodeLabel) {
                try {
                    unstashFiles script: script, stage: stageName
                    executeStage(body, stageName, mergedStageConfiguration, generalConfiguration)
                    stashFiles script: script, stage: stageName
                    echo "Current build result in stage $stageName is ${script.currentBuild.result}."
                } finally {
                    deleteDir()
                }
            }
        }
    }
}

private executeStage(Closure originalStage, String stageName, Map stageConfiguration, Map generalConfiguration) {
    /* Defining the sources where to look for a project extension and a repository extension.
     * Files need to be named like the executed stage to be recognized.
     */
    def projectInterceptorFile = "${s4SdkGlobals.projectExtensionsDirectory}/${stageName}.groovy"
    def repositoryInterceptorFile = "${s4SdkGlobals.repositoryExtensionsDirectory}/${stageName}.groovy"

    // Pre-defining the real originalStage in body variable, might be overwritten later if extensions exist
    def body = originalStage

    // First, check if a repository extension exists
    if (fileExists(repositoryInterceptorFile)) {
        Script repositoryInterceptorScript = load(repositoryInterceptorFile)
        echo "Found repository interceptor for ${stageName}."
        // If we call the repository interceptor, we will pass on originalStage as parameter
        body = {
            repositoryInterceptorScript(originalStage, stageName, stageConfiguration, generalConfiguration)
        }
    }

    // Second, check if a project extension exists
    if (fileExists(projectInterceptorFile)) {
        Script projectInterceptorScript = load(projectInterceptorFile)
        echo "Found project interceptor for ${stageName}."
        // If we call the project interceptor, we will pass on body as parameter which contains either originalStage or the repository interceptor
        projectInterceptorScript(body, stageName, stageConfiguration, generalConfiguration)
    } else {
        // This calls either originalStage if no interceptors where found, or repository interceptor if no project interceptor was found
        body()
    }
}

