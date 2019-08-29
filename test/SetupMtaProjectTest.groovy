import com.lesfurets.jenkins.unit.BasePipelineTest
import com.sap.cloud.sdk.s4hana.pipeline.mock.NullScript
import com.sap.cloud.sdk.s4hana.pipeline.util.BaseCloudSdkTest
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

class SetupMtaProjectTest extends BaseCloudSdkTest {

    Script dummyScript

    @Before
    void prepareTests() throws Exception {
        setUp()
        dummyScript = new NullScript(this)

        helper.registerAllowedMethod("readYaml", [Map.class], {
            ['ID'     : 'someId',
             'modules':
                 [
                     ['name': 'some-srv', 'type': 'java', 'path': 'srv']
                 ]
            ]
        })
    }

    @Test
    void 'MTA structure without unit- and integration tests, should be accepted'() {
        helper.registerAllowedMethod('fileExists', [String.class], { String filename ->

            if (filename.endsWith('srv/integration-tests')) {
                return false
            }

            if (filename == './integration-tests') {
                return true
            }

            if (filename.endsWith("srv/unit-tests")) {
                return false
            }

            throw new RuntimeException("Did not expect to get here in this test.")
        })

        def script = loadScript("vars/setupMtaProject.groovy")
        script.invokeMethod("call", [script: dummyScript, generalConfiguration: [:]])

        assertEquals('SUCCESS', binding.getVariable('currentBuild').result)
    }

    @Test
    void 'MTA structure with unit- and integration tests, should fail'() {
        helper.registerAllowedMethod('fileExists', [String.class], { String filename ->
            if (filename.endsWith('srv/integration-tests')) {
                return true
            }

            if (filename == './integration-tests') {
                return true
            }

            if (filename.endsWith("srv/unit-tests")) {
                return false
            }

            throw new RuntimeException("Did not expect to get here in this test.")
        })

        def script = loadScript("vars/setupMtaProject.groovy")
        script.invokeMethod("call", [script: dummyScript, generalConfiguration: [:]])

        assertEquals('FAILURE', binding.getVariable('currentBuild').result)
    }

    @Test
    void 'MTA structure with unit tests, should fail'() {
        helper.registerAllowedMethod('fileExists', [String.class], { String filename ->
            if (filename.endsWith('srv/integration-tests')) {
                return false
            }

            if (filename == './integration-tests') {
                return true
            }

            if (filename.endsWith("srv/unit-tests")) {
                return true
            }

            throw new RuntimeException("Did not expect to get here in this test.")
        })

        def script = loadScript("vars/setupMtaProject.groovy")
        script.invokeMethod("call", [script: dummyScript, generalConfiguration: [:]])

        assertEquals('FAILURE', binding.getVariable('currentBuild').result)
    }

    @Test
    void 'MTA structure with integration tests, should fail'() {
        helper.registerAllowedMethod('fileExists', [String.class], { String filename ->
            if (filename.endsWith('srv/integration-tests')) {
                return true
            }

            if (filename == './integration-tests') {
                return true
            }

            if (filename.endsWith("srv/unit-tests")) {
                return true
            }

            throw new RuntimeException("Did not expect to get here in this test.")
        })

        def script = loadScript("vars/setupMtaProject.groovy")
        script.invokeMethod("call", [script: dummyScript, generalConfiguration: [:]])

        assertEquals('FAILURE', binding.getVariable('currentBuild').result)
    }
}
