package com.sap.icd.jenkins

import com.cloudbees.groovy.cps.NonCPS

class ConfigurationLoader implements Serializable {
    @NonCPS
    def static stepConfiguration(script, String stepName) {
        return loadConfiguration(script, 'steps', stepName)
    }

    @NonCPS
    def static stageConfiguration(script, String stageName) {
        return loadConfiguration(script, 'stages', stageName)
    }

    @NonCPS
    def static generalConfiguration(script){
        return script?.pipelineEnvironment?.configuration?.general ?: [:]
    }

    private static loadConfiguration(script, String type, String entryName){
        return script?.pipelineEnvironment?.configuration?.get(type)?.get(entryName) ?: [:]
    }
}
