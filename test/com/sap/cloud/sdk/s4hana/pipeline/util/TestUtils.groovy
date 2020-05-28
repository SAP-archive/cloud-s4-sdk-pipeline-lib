package com.sap.cloud.sdk.s4hana.pipeline.util

import com.lesfurets.jenkins.unit.BasePipelineTest

class TestUtils {
    static registerStep(BasePipelineTest test, String stepName){
        Script step = test.helper.loadScript("${stepName}.groovy")
        test.helper.registerAllowedMethod(stepName, [Map.class], { Map parameters ->
            step.call(parameters)
        })
    }
}
