package com.sap.cloud.sdk.s4hana.pipeline

import org.junit.Before
import org.junit.Test
import static org.junit.Assert.*

class deployToCloudPlatformTest extends com.sap.cloud.sdk.s4hana.pipeline.util.BaseCloudSdkTest {

    @Before
    void prepareTests() throws Exception {
        setUp()
    }

    @Test
    void 'If application name contains underscore it should give error '() {

        String errorMessage
        String message = "Your application name contains non-alphanumeric character i.e 'underscore'. Please rename test_App that it does not contain any non-alphanumeric characters, as they are not supported by CloudFoundry.. \n" +
            "For more details please visit https://docs.cloudfoundry.org/devguide/deploy-apps/deploy-app.html#basic-settings"
        boolean errorCalled = false

        helper.registerAllowedMethod('handleStepErrors', [Map.class, Closure.class], { Map parameters, Closure closure ->
            closure.call()
        })
        helper.registerAllowedMethod('error', [String.class], { String str ->
            errorCalled = true
            errorMessage = str
        })
        helper.registerAllowedMethod('runClosures', [Map.class, Script.class], { Map parameters, Script script ->

        })

        Script script = loadScript("vars/deployToCloudPlatform.groovy")
        script.invokeMethod("call", [script   : dummyScript,
                                     cfTargets: [appName: "test_App"]
        ])

        assertTrue(errorCalled)
        assertEquals(message, errorMessage)
    }

    @Test
    void 'If application name contains non alphanumeric characters it should give warning '() {

        String warningMessage
        String message = "Your application name contains non-alphanumeric characters that may lead to errors in the future, as they are not supported by CloudFoundry. \n" +
            "For more details please visit https://docs.cloudfoundry.org/devguide/deploy-apps/deploy-app.html#basic-settings"
        boolean warningCalled = false

        helper.registerAllowedMethod('handleStepErrors', [Map.class, Closure.class], { Map parameters, Closure closure ->
            closure.call()
        })
        helper.registerAllowedMethod('addBadge', [Map.class], { Map params ->
            warningCalled = true
            warningMessage = params.text
        })
        helper.registerAllowedMethod('runClosures', [Map.class, Script.class], { Map parameters, Script script ->

        })

        Script script = loadScript("vars/deployToCloudPlatform.groovy")
        script.invokeMethod("call", [script   : dummyScript,
                                     cfTargets: [appName: "test-App"]
        ])

        assertTrue(warningCalled)
        assertEquals(message, warningMessage)
    }
}
