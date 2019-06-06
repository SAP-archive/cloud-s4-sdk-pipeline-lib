import com.cloudbees.groovy.cps.NonCPS
import com.sap.piper.ConfigurationHelper
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger
import com.sap.piper.k8s.ContainerMap
import hudson.model.Result

import groovy.transform.Field

@Field String STEP_NAME = 'runAsStage'

@Field Set PARAMETER_KEYS = ['node']


def call(Map parameters = [:], body) {

    Map configurationHelper = ConfigurationHelper.newInstance(this, parameters)
        .withMandatoryProperty('stageName')
        .withMandatoryProperty('script')
        .use()

    String stageName = configurationHelper.stageName
    Script script = configurationHelper.script

    Map configuration = ConfigurationHelper.newInstance(this)
        .loadStepDefaults()
        .mixin(ConfigurationLoader.defaultStageConfiguration(script, stageName))
        .mixinGeneralConfig(script.commonPipelineEnvironment)
        .mixinStageConfig(script.commonPipelineEnvironment, stageName)
        .mixin(parameters, PARAMETER_KEYS)
        .use()

    configuration.uniqueId = UUID.randomUUID().toString()
    String nodeLabel = configuration.defaultNode

    Map containerMap = ContainerMap.instance.getMap().get(stageName) ?: [:]
    if (configuration.node) {
        nodeLabel = configuration.node
    }

    handleStepErrors(stepName: stageName, stepParameters: [:]) {
        if (Boolean.valueOf(env.ON_K8S) && containerMap.size() > 0) {
            withEnv(["POD_NAME=${stageName}"]) {
                dockerExecuteOnKubernetes(script: script, containerMap: containerMap) {
                    executeStage(script, body, stageName, configuration)
                }
            }
        } else {
            node(nodeLabel) {
                try {
                    executeStage(script, body, stageName, configuration)
                } finally {
                    deleteDir()
                }
            }
        }
    }
}

private executeStage(Script script,
                     Closure originalStage,
                     String stageName,
                     Map configuration) {
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
            echo "Found repository interceptor for ${stageName}."
            // If we call the repository interceptor, we will pass on originalStage as parameter
            body = {
                callInterceptor(script, repositoryInterceptorFile, originalStage, stageName, configuration)
            }
        }

        // Second, check if a project extension exists
        if (projectExtensions) {
            echo "Found project interceptor for ${stageName}."
            // If we call the project interceptor, we will pass on body as parameter which contains either originalStage or the repository interceptor
            callInterceptor(script, projectInterceptorFile, body, stageName, configuration)
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


private callInterceptor(Script script, String extensionFileName, Closure originalStage, String stageName, Map configration){
    Script interceptor = load(extensionFileName)
    //TODO: Remove handling of legacy interface
    if(isOldInterceptorInterfaceUsed(interceptor)){
        echo("[Warning] The interface to implement extensions has changed. " +
            "The extension $extensionFileName has to implement a method named 'call' with exactly one parameter of type Map. " +
            "This map will have the properties script, originalStage, stageName, config. " +
            "For example: def call(Map parameters) { ... }")
        interceptor(originalStage, stageName, configration, configration)
    }
    else {
        validateInterceptor(interceptor, extensionFileName)
        interceptor([
            script: script,
            originalStage:originalStage,
            stageName: stageName,
            config: configration
        ])
    }
}

@NonCPS
private boolean isInterceptorValid(Script interceptor){
    MetaMethod method = interceptor.metaClass.pickMethod("call", [Map.class] as Class[])
    return method != null
}

private validateInterceptor(Script interceptor, String extensionFileName){
    if(!isInterceptorValid(interceptor)){
        error("The extension $extensionFileName has to implement a method named 'call' with exactly one parameter of type Map. " +
            "This map will have the properties script, originalStage, stageName, config. " +
            "For example: def call(Map parameters) { ... }")
    }
}

@NonCPS
private boolean isOldInterceptorInterfaceUsed(Script interceptor){
    MetaMethod method = interceptor.metaClass.pickMethod("call", [Closure.class, String.class, Map.class, Map.class] as Class[])
    return method != null
}

