package com.sap.cloud.sdk.s4hana.pipeline

@Singleton
class BuildToolEnvironment implements Serializable{
    BuildTool buildTool
    Map<String, List<String>> modules

    boolean isMta(){
        buildTool == BuildTool.MTA
    }

    boolean isNpm(){
        buildTool == BuildTool.NPM
    }

    boolean isMaven(){
        buildTool == BuildTool.MAVEN
    }

    List<String> getModulesPathOfType(String moduleType){
        if(isMta()){
            return modules.get(moduleType)?:[]
        }
        else {
            return ["./"]
        }
    }
}
