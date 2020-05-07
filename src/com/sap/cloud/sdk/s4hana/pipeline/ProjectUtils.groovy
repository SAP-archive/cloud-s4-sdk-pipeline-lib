package com.sap.cloud.sdk.s4hana.pipeline

import com.sap.piper.ConfigurationLoader

class ProjectUtils implements Serializable {
    static String getProjectName(Script script) {
        Map generalConfiguration = ConfigurationLoader.generalConfiguration(script)
        String projectName = generalConfiguration.projectName?.trim() ?: getProjectNameFromBuildDescriptor(script)
        if(!projectName){
            script.error "This should not happen: Project name was not specified in the configuration and could not be derived from the project."
        }
        return projectName
    }

    private static String getProjectNameFromBuildDescriptor(Script script) {
        if (BuildToolEnvironment.instance.isMta()) {
            Map mta = readYaml file: 'mta.yaml'
            return mta.ID
        } else if (BuildToolEnvironment.instance.isMaven()) {
            def pom = readMavenPom file: 'pom.xml'
            return "${pom.groupId}-${pom.artifactId}"
        } else if (BuildToolEnvironment.instance.isNpm()) {
            Map packageJson = readJSON file: 'package.json'
            return packageJson.name
        }
        script.error "Project build tool was none of the expected ones MTA, maven, npm."
    }
}
