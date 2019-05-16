package com.sap.cloud.sdk.s4hana.pipeline

@Singleton
class BuildToolEnvironment implements Serializable{
    BuildTool buildTool
    Map modules

    boolean isMta(){
        buildTool == BuildTool.MTA
    }

    boolean isNpm(){
        buildTool == BuildTool.NPM
    }

    boolean isMaven(){
        buildTool == BuildTool.MAVEN
    }

    List getModulesPathOfType(String moduleType){
        if(isMta()){
            return modules.get(moduleType)?:[]
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
