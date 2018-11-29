import com.sap.piper.ConfigurationHelper
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger
import com.sap.piper.k8s.ContainerMap
import hudson.model.Result

import java.util.UUID

import groovy.transform.Field

@Field String STEP_NAME = 'runAsStage'

def call(Map parameters = [:], body) {
    Map configurationHelper = ConfigurationHelper.newInstance(this, parameters)
        .withMandatoryProperty('stageName')
        .withMandatoryProperty('script')
        .use()
    def stageName = configurationHelper.stageName
    def script = configurationHelper.script
    Map defaultGeneralConfiguration = ConfigurationLoader.defaultGeneralConfiguration(script)
    Map projectGeneralConfiguration = ConfigurationLoader.generalConfiguration(script)

    Map generalConfiguration = ConfigurationMerger.merge(
        projectGeneralConfiguration,
        projectGeneralConfiguration.keySet(),
        defaultGeneralConfiguration)

    Map stageDefaultConfiguration = ConfigurationLoader.defaultStageConfiguration(script, stageName)
    Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)

    Set parameterKeys = ['node']
    Map mergedStageConfiguration = ConfigurationMerger.merge(
        parameters,
        parameterKeys,
        stageConfiguration,
        stageConfiguration.keySet(),
        stageDefaultConfiguration)
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
                    executeStage(script, body, stageName, mergedStageConfiguration, generalConfiguration)
                }
            }
        } else {
            node(nodeLabel) {
                try {
                    executeStage(script, body, stageName, mergedStageConfiguration, generalConfiguration)
                } finally {
                    deleteDir()
                }
            }
        }
    }
}

private executeStage(def script,
                     Closure originalStage,
                     String stageName,
                     Map stageConfiguration,
                     Map generalConfiguration) {
    boolean projectExtensions
    boolean globalExtensions
    def startTime = System.currentTimeMillis()
    try {
        unstashFiles script: script, stage: stageName

        /* Defining the sources where to look for a project extension and a repository extension.
         * Files need to be named like the executed stage to be recognized.
         */
        def projectInterceptorFile = "${s4SdkGlobals.projectExtensionsDirectory}/${stageName}.groovy"
        def repositoryInterceptorFile = "${s4SdkGlobals.repositoryExtensionsDirectory}/${stageName}.groovy"
        projectExtensions = fileExists(projectInterceptorFile)
        globalExtensions = fileExists(repositoryInterceptorFile)
        // Pre-defining the real originalStage in body variable, might be overwritten later if extensions exist
        def body = originalStage

        // First, check if a repository extension exists
        if (globalExtensions) {
            Script repositoryInterceptorScript = load(repositoryInterceptorFile)
            echo "Found repository interceptor for ${stageName}."
            // If we call the repository interceptor, we will pass on originalStage as parameter
            body = {
                repositoryInterceptorScript(originalStage, stageName, stageConfiguration, generalConfiguration)
            }
        }

        // Second, check if a project extension exists
        if (projectExtensions) {
            Script projectInterceptorScript = load(projectInterceptorFile)
            echo "Found project interceptor for ${stageName}."
            // If we call the project interceptor, we will pass on body as parameter which contains either originalStage or the repository interceptor
            projectInterceptorScript(body, stageName, stageConfiguration, generalConfiguration)
        } else {
            // This calls either originalStage if no interceptors where found, or repository interceptor if no project interceptor was found
            body()
        }
        stashFiles script: script, stage: stageName
        echo "Current build result in stage $stageName is ${Result.fromString(currentBuild.currentResult)}."
    } finally {
        prepareAndSendAnalytics(script, stageName, startTime, projectExtensions, globalExtensions)
    }
}

private prepareAndSendAnalytics(def script, String stageName, def startTime, boolean projectExtensions, boolean globalExtensions) {
    def stageInfo = [:]
    stageInfo.event_type = 'pipeline_stage'
    stageInfo.custom3 = 'stage_name'
    stageInfo.e_3 = stageName

    stageInfo.custom4 = 'stage_result'
    stageInfo.e_4 = Result.fromString(currentBuild.currentResult)

    stageInfo.custom5 = 'start_time'
    stageInfo.e_5 = startTime

    stageInfo.custom6 = 'duration'
    stageInfo.e_6 = System.currentTimeMillis() - startTime

    stageInfo.custom7 = 'project_extensions'
    stageInfo.e_7 = projectExtensions

    stageInfo.custom8 = 'global_extensions'
    stageInfo.e_8 = globalExtensions

    sendAnalytics(script: script, telemetryData: stageInfo)
}
