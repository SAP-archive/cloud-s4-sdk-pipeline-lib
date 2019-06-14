import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

class SetupMtaProjectTest extends BasePipelineTest {

    Script dummyScript = null

    @Before
    void prepareTests() throws Exception {
        setUp()
        dummyScript = new Script() {
            def commonPipelineEnvironment = ['configuration': ['artifactId': 'irrelevant']]

            @Override
            Object run() {
                return null
            }
        }
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
            if (filename.endsWith('/integration-tests')) {
                return false
            }

            if (filename.endsWith("/unit-tests")) {
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
            if (filename.endsWith('/integration-tests')) {
                return true
            }

            if (filename.endsWith("/unit-tests")) {
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
            if (filename.endsWith('/integration-tests')) {
                return false
            }

            if (filename.endsWith("/unit-tests")) {
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
            if (filename.endsWith('/integration-tests')) {
                return true
            }

            if (filename.endsWith("/unit-tests")) {
                return true
            }

            throw new RuntimeException("Did not expect to get here in this test.")
        })

        def script = loadScript("vars/setupMtaProject.groovy")
        script.invokeMethod("call", [script: dummyScript, generalConfiguration: [:]])

        assertEquals('FAILURE', binding.getVariable('currentBuild').result)
    }
}
