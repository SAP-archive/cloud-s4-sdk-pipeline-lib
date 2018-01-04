package com.sap.cloud.sdk.s4hana.pipeline

import com.cloudbees.groovy.cps.NonCPS

class ManifestUpdater implements Serializable{
    private Map manifest

    ManifestUpdater(Map manifest){
        this.manifest = manifest
    }

    Map getManifest(){
        return manifest
    }

    void addEnvironmentsVariables(Map environmentVariables){
        if(manifest.applications){
            for(int i=0; i<manifest.applications.size(); i++){
                Map application = manifest.applications[i]
                addEnvironmentsVariablesToApplication(application, environmentVariables)
            }
        }
    }

    @NonCPS
    private addEnvironmentsVariablesToApplication(Map application, Map environmentVariables){
        if(application.env){
            Set environmentVariableKeys = environmentVariables.keySet()
            for(int i=0; i<environmentVariableKeys.size(); i++){
                String environmentVariableKey = environmentVariableKeys[i]
                application.env.putIfAbsent(environmentVariableKey, environmentVariables[environmentVariableKey])
            }
        }
        else {
            application.env = environmentVariables
        }
    }
}
