package com.sap.cloud.sdk.s4hana.pipeline

enum DeploymentType {
    ROLLING_UPDATE('rolling-update'), BLUE_GREEN('blue-green'), STANDARD('standard')

    private String value

    public DeploymentType(String value){
        this.value = value
    }

    @Override
    public String toString(){
        return value
    }

    static DeploymentType selectFor(CloudPlatform cloudPlatform, boolean isProduction) {
        if (!isProduction) {
            return STANDARD
        } else {
            switch (cloudPlatform) {
                case CloudPlatform.NEO:
                    return ROLLING_UPDATE
                case CloudPlatform.CLOUD_FOUNDRY:
                    return BLUE_GREEN
                default:
                    throw new RuntimeException("Unknown cloud platform: ${cloudPlatform}")
            }
        }
    }
}
