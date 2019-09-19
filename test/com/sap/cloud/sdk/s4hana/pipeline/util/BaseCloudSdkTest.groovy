package com.sap.cloud.sdk.s4hana.pipeline.util

import com.lesfurets.jenkins.unit.BasePipelineTest
import com.sap.cloud.sdk.s4hana.pipeline.mock.NullScript

class BaseCloudSdkTest extends BasePipelineTest {

    Script dummyScript

    BaseCloudSdkTest() {
        scriptRoots += 'src/com/sap/cloud/sdk/s4hana/pipeline/'
        dummyScript = new NullScript(this)
    }

}
