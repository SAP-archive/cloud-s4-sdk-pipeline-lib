package com.sap.cloud.sdk.s4hana.pipeline

enum DeploymentType {
    NEO_ROLLING_UPDATE('rolling-update'), CF_BLUE_GREEN('blue-green'), CF_STANDARD('standard'), NEO_DEPLOY('deploy')

    private String value

    public DeploymentType(String value){
        this.value = value
    }

    @Override
    public String toString(){
        return value
    }

    static DeploymentType selectFor(CloudPlatform cloudPlatform, boolean isProduction) {
        switch (cloudPlatform) {
            case CloudPlatform.NEO:
                if (!isProduction) {
                    return NEO_DEPLOY
                }
                return NEO_ROLLING_UPDATE
            case CloudPlatform.CLOUD_FOUNDRY:
                if (!isProduction) {
                    return CF_STANDARD
                }
                return CF_BLUE_GREEN
            default:
                throw new RuntimeException("Unknown cloud platform: ${cloudPlatform}")
        }
    }
}
