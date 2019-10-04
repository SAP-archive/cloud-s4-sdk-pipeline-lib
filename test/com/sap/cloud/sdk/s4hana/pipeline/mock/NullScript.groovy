package com.sap.cloud.sdk.s4hana.pipeline.mock

import com.lesfurets.jenkins.unit.BasePipelineTest
import com.lesfurets.jenkins.unit.PipelineTestHelper

class NullScript extends Script {
    BasePipelineTest test
    Map commonPipelineEnvironment = ['configuration': ['artifactId': 'irrelevant']]

    NullScript(BasePipelineTest test) {
        this.test = test
        PipelineTestHelper helper = test.getHelper()
        this.metaClass.invokeMethod = helper.getMethodInterceptor()
        this.metaClass.static.invokeMethod = helper.getMethodInterceptor()
        this.metaClass.methodMissing = helper.getMethodMissingInterceptor()
    }

    def run(){

    }

}
