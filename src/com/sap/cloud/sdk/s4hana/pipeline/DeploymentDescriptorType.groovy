package com.sap.cloud.sdk.s4hana.pipeline

enum DeploymentDescriptorType {

    MTA('mta.yaml'), NEOTARGETS('pipeline_config.yml'), MANIFEST('manifest.yml')

    final String descriptorFilename

    DeploymentDescriptorType(String descriptorFilename) {
        this.descriptorFilename = descriptorFilename
    }

}
