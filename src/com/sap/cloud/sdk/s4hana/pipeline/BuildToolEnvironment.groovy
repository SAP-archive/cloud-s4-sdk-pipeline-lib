package com.sap.cloud.sdk.s4hana.pipeline

@Singleton
class BuildToolEnvironment implements Serializable{
    BuildTool buildTool
    Map modulesMap
    List npmModules = []

    boolean isMta() {
        buildTool == BuildTool.MTA
    }

    boolean isNpm() {
        buildTool == BuildTool.NPM
    }

    boolean isMaven() {
        buildTool == BuildTool.MAVEN
    }

    List getModulesPathOfType(List moduleTypes) {
        List modulesList = []
        if (isMta()) {
            for (int i = 0; i < moduleTypes.size(); i++) {
                String moduleType = moduleTypes[i]
                modulesList.addAll(modulesMap.get(moduleType) ?: [])
            }

            return modulesList
        } else {
            return ["./"]
        }
    }

    String getUnitTestPath(String basePath) {
        if (isMta()) {
            return basePath
        } else {
            return PathUtils.normalize(basePath, "/unit-tests")
        }
    }

    String getApplicationPath(String basePath) {
        if (isMta()) {
            return basePath
        } else {
            return PathUtils.normalize(basePath, "/application")
        }
    }

    String getApplicationPomXmlPath(String basePath) {
        return PathUtils.normalize(getApplicationPath(basePath), "pom.xml")
    }


    boolean isMtaWithIntegrationTests(Script script) {
        return isMta() && script.fileExists("./integration-tests")
    }

    List getNpmModulesWithScripts(List scriptsToCheck) {
        List filteredNpmModules = []

        for (int i = 0; i < npmModules.size(); i++) {
            if (npmModules[i].npmScripts.any { npmScriptFromModule -> scriptsToCheck.contains(npmScriptFromModule) }) {
                filteredNpmModules.add(npmModules[i])
            }
        }
        return filteredNpmModules
    }
}
