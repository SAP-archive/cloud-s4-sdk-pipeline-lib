package com.sap.cloud.sdk.s4hana.pipeline

enum DeploymentType {
    ROLLING_UPDATE, BLUE_GREEN, STANDARD

    static DeploymentType selectFor(CloudPlatform cloudPlatform, boolean isProduction, boolean forceDowntimeDeployment = false) {
        if ((!isProduction) || forceDowntimeDeployment) {
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
