package com.sap.cloud.sdk.s4hana.pipeline

import com.sap.cloud.sdk.s4hana.pipeline.util.BaseCloudSdkTest
import com.sap.piper.ConfigurationHelper
import com.sap.piper.ConfigurationLoader
import org.junit.Before
import org.junit.Test
import static org.junit.Assert.*

class stageSonarQubeScanTest extends BaseCloudSdkTest {

    @Before
    void prepareTests() throws Exception {
        setUp()
    }

    @Test
    void 'Verify that user provided sonarqube properties is used'() {

        List expectedProperties = ['sonar.java.binaries=', 'sonar.coverage.exclusions=**.js,unit-tests/**,integration-tests/**,performance-tests/**,**.xml,**/target/**', 'sonar.example = test', 'sonar.prop = test', 'sonar.projectKey=testProject']
        List actualProperties

        helper.registerAllowedMethod('runAsStage', [Map.class, Closure.class], { Map parameters, Closure closure ->
            closure.call()
        })

        helper.registerAllowedMethod('runOverModules', [Map.class, Closure.class], { Map parameters, Closure closure ->
        })

        helper.registerAllowedMethod('executeNpm', [Map.class, Closure.class], { Map parameters, Closure closure ->
            closure.call()
        })

        helper.registerAllowedMethod('sonarExecuteScan', [Map.class], { Map parameters ->
            actualProperties = parameters.options
        })

        Script script = loadScript("vars/stageSonarQubeScan.groovy")
        script.commonPipelineEnvironment = [:]
        script.commonPipelineEnvironment.configuration = [:]
        script.commonPipelineEnvironment.configuration.stages = [:]
        script.commonPipelineEnvironment.configuration.stages.sonarQubeScan = [
            projectKey: 'testProject', instance: 'sonarInstance', sonarProperties: ['sonar.example = test', 'sonar.prop = test']
        ]
        script.invokeMethod("call", [script: script])
        assertEquals(expectedProperties, actualProperties)
    }

    @Test
    void 'Verifying the instance'() {

        String actualInstance

        helper.registerAllowedMethod('runAsStage', [Map.class, Closure.class], { Map parameters, Closure closure ->
            closure.call()
        })

        helper.registerAllowedMethod('runOverModules', [Map.class, Closure.class], { Map parameters, Closure closure ->
        })

        helper.registerAllowedMethod('executeNpm', [Map.class, Closure.class], { Map parameters, Closure closure ->
            closure.call()
        })

        helper.registerAllowedMethod('sonarExecuteScan', [Map.class], { Map parameters ->
            actualInstance = parameters.instance
        })

        Script script = loadScript("vars/stageSonarQubeScan.groovy")
        script.commonPipelineEnvironment = [:]
        script.commonPipelineEnvironment.configuration = [:]
        script.commonPipelineEnvironment.configuration.stages = [:]
        script.commonPipelineEnvironment.configuration.stages.sonarQubeScan = [
            projectKey: 'testProject', instance: 'sonarInstance'
        ]

        script.invokeMethod("call", [script: script])
        assertEquals("sonarInstance", actualInstance)
    }

    @Test
    void 'Check code echo when projectKey is empty and set it to unnamed'() {

        String echoMessage
        String message = "Please provide projectKey in configuration for SonarQube"


        List expectedProperties = ['sonar.java.binaries=', 'sonar.coverage.exclusions=**.js,unit-tests/**,integration-tests/**,performance-tests/**,**.xml,**/target/**', 'sonar.example = test', 'sonar.prop = test', 'sonar.projectKey=unnamed']
        List actualProperties

        helper.registerAllowedMethod('runAsStage', [Map.class, Closure.class], { Map parameters, Closure closure ->
            closure.call()
        })

        helper.registerAllowedMethod('runOverModules', [Map.class, Closure.class], { Map parameters, Closure closure ->
        })

        helper.registerAllowedMethod('executeNpm', [Map.class, Closure.class], { Map parameters, Closure closure ->
            closure.call()
        })

        helper.registerAllowedMethod('sonarExecuteScan', [Map.class], { Map parameters ->
            actualProperties = parameters.options
        })

        helper.registerAllowedMethod('echo', [String.class], { String str ->
            echoMessage = str
        })

        Script script = loadScript("vars/stageSonarQubeScan.groovy")
        script.commonPipelineEnvironment = [:]
        script.commonPipelineEnvironment.configuration = [:]
        script.commonPipelineEnvironment.configuration.stages = [:]
        script.commonPipelineEnvironment.configuration.stages.sonarQubeScan = [
            projectKey: '', instance: 'sonarInstance', sonarProperties: ['sonar.example = test', 'sonar.prop = test']
        ]
        script.invokeMethod("call", [script: script])

        assertEquals(expectedProperties, actualProperties)
        assertEquals(message, echoMessage)
    }
}
