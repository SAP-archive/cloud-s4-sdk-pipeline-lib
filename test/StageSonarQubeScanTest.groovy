import com.sap.cloud.sdk.s4hana.pipeline.util.BaseCloudSdkTest
import com.sap.cloud.sdk.s4hana.pipeline.util.TestUtils
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertArrayEquals
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat

class StageSonarQubeScanTest extends BaseCloudSdkTest {

    @Before
    void prepareTests() throws Exception {
        setUp()

        helper.registerAllowedMethod('piperStageWrapper', [Map.class, Closure.class], { Map parameters, Closure closure ->
            closure.call()
        })

        helper.registerAllowedMethod('executeNpm', [Map.class, Closure.class], { Map parameters, Closure closure ->
            closure.call()
        })

        TestUtils.registerStep(this, 'loadEffectiveStageConfiguration')

        helper.registerAllowedMethod('findFiles', [Map.class], { Map parameters ->
            switch (parameters.glob){
                case '**/target/**/*.exec':
                    return [[path:"s4hana_pipeline/report.exec"]]
                case '**/pom.xml':
                    return [[path:"application/pom.xml"]]

                default:
                    throw new Exception("")
            }
        })

        helper.registerAllowedMethod('fileExists', [String.class], { String path ->
            List existingFiles = ['application/target/classes/']
            return existingFiles.contains(path)
        })
    }

    @Test
    void 'Verify that user provided sonarqube properties are used'() {

        List actualProperties
        helper.registerAllowedMethod('sonarExecuteScan', [Map.class], { Map parameters ->
            actualProperties = parameters.options
        })

        Script script = loadScript("vars/stageSonarQubeScan.groovy")
        script.commonPipelineEnvironment = [configuration: [stages: [ sonarQubeScan : [
            projectKey: 'testProject',
            instance: 'sonarInstance',
            sonarProperties: ['sonar.example = test', 'sonar.prop = test']
        ]]]]

        script.invokeMethod("call", [script: script])

        List expectedProperties = [
            'sonar.java.binaries=application/target/classes/',
            'sonar.coverage.exclusions=**.js,unit-tests/**,integration-tests/**,performance-tests/**,**.xml,**/target/**',
            'sonar.jacoco.reportPaths=s4hana_pipeline/report.exec',
            'sonar.java.libraries=./s4hana_pipeline/maven_local_repo/**',
            'sonar.example = test',
            'sonar.prop = test',
            'sonar.projectKey=testProject'
        ]
        assertEquals(expectedProperties, actualProperties)
    }

    @Test
    void 'Verifying the instance'() {

        String actualInstance

        helper.registerAllowedMethod('sonarExecuteScan', [Map.class], { Map parameters ->
            actualInstance = parameters.instance
        })

        Script script = loadScript("vars/stageSonarQubeScan.groovy")

        script.commonPipelineEnvironment = [configuration: [stages: [ sonarQubeScan : [
            projectKey: 'testProject',
            instance: 'sonarInstance'
        ]]]]

        script.invokeMethod("call", [script: script])
        assertEquals("sonarInstance", actualInstance)
    }
}
