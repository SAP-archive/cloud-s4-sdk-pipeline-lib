package com.sap.cloud.sdk.s4hana.pipeline

import com.cloudbees.groovy.cps.NonCPS

class ConfigurationLoader implements Serializable {
    @NonCPS
    def static stepConfiguration(script, String stepName) {
        return loadConfiguration(script, 'steps', stepName, ConfigurationType.CUSTOM_CONFIGURATION)
    }

    @NonCPS
    def static stageConfiguration(script, String stageName) {
        return loadConfiguration(script, 'stages', stageName, ConfigurationType.CUSTOM_CONFIGURATION)
    }

    @NonCPS
    def static postActionConfiguration(script, String actionName){
        return loadConfiguration(script, 'postActions', actionName, ConfigurationType.CUSTOM_CONFIGURATION)
    }

    @NonCPS
    def static defaultStepConfiguration(script, String stepName) {
        return loadConfiguration(script, 'steps', stepName, ConfigurationType.DEFAULT_CONFIGURATION)
    }

    @NonCPS
    def static defaultStageConfiguration(script, String stageName) {
        return loadConfiguration(script, 'stages', stageName, ConfigurationType.DEFAULT_CONFIGURATION)
    }

    @NonCPS
    def static generalConfiguration(script){
        return script?.pipelineEnvironment?.configuration?.general ?: [:]
    }

    @NonCPS
    def static defaultGeneralConfiguration(script){
        return script?.pipelineEnvironment?.defaultConfiguration?.general ?: [:]
    }

    @NonCPS
    private static loadConfiguration(script, String type, String entryName, ConfigurationType configType){
        switch (configType) {
            case ConfigurationType.CUSTOM_CONFIGURATION:
                return script?.pipelineEnvironment?.configuration?.get(type)?.get(entryName) ?: [:]
            case ConfigurationType.DEFAULT_CONFIGURATION:
                return script?.pipelineEnvironment?.defaultConfiguration?.get(type)?.get(entryName) ?: [:]
            default:
                throw new RuntimeException("Unknown configuration type: ${configType}")
        }
    }
}
