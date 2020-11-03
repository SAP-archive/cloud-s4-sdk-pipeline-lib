package com.sap.cloud.sdk.s4hana.pipeline

import com.sap.piper.ConfigurationLoader

class ProjectUtils implements Serializable {
    static String getProjectName(Script script) {
        Map generalConfiguration = ConfigurationLoader.generalConfiguration(script)
        String projectName = generalConfiguration.projectName?.trim() ?: script.commonPipelineEnvironment.projectName
        if(!projectName){
            script.error "This should not happen: Project name was not specified in the configuration and could not be derived from the project."
        }
        return projectName
    }
}
