package com.sap.cloud.sdk.s4hana.pipeline

enum BuildTool {
    MAVEN('maven'), NPM('npm'), MTA('mta')

    String piperBuildTool

    public BuildTool(String piperBuildTool){
        this.piperBuildTool = piperBuildTool
    }

    String getPiperBuildTool(){
        return piperBuildTool
    }
}
