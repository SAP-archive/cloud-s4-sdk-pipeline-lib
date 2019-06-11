package com.sap.cloud.sdk.s4hana.pipeline

@Singleton
class BuildToolEnvironment implements Serializable{
    BuildTool buildTool
    Map modulesMap

    boolean isMta(){
        buildTool == BuildTool.MTA
    }

    boolean isNpm(){
        buildTool == BuildTool.NPM
    }

    boolean isMaven(){
        buildTool == BuildTool.MAVEN
    }

    List getModulesPathOfType(List moduleTypes){
        List modulesList = []
        if(isMta()){
            for(int i=0; i<moduleTypes.size(); i++){
                String moduleType = moduleTypes[i]
                modulesList.addAll(modulesMap.get(moduleType)?:[])
            }

            return modulesList
        }
        else {
            return ["./"]
        }
    }

    String getUnitTestPath() {
        if (isMta()) {
            return "/srv/application"
        } else {
            return "/unit-tests"
        }
    }
}
