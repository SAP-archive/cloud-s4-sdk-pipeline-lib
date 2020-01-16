import com.sap.cloud.sdk.s4hana.pipeline.BuildTool
import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.util.BaseCloudSdkTest
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

class StageUnitTestsTest extends BaseCloudSdkTest {
    @Before
    void prepareTests() throws Exception {
        setUp()
        helper.registerAllowedMethod("runAsStage", [Map.class, Closure.class], null)
        helper.registerAllowedMethod("injectQualityListenerDependencies", [Map.class], null)
        helper.registerAllowedMethod("mavenExecute", [Map.class], null)
        helper.registerAllowedMethod("executeNpm", [Map.class, Closure.class], null)
        helper.registerAllowedMethod("copyExecFile", [Map.class], null)
        helper.registerAllowedMethod("collectJUnitResults", [Map.class, Closure.class], null)
        this.binding.setVariable('s4SdkGlobals', new s4SdkGlobals())
    }

    @Test
    void 'Npm Project should not run over Modules'() {
        boolean runOverNpmModulesCalled = false
        BuildToolEnvironment.instance.buildTool = BuildTool.NPM
        helper.registerAllowedMethod("runOverModules", [Map.class, Closure.class], { fail("May not run over [mta] modules in a npm project") })
        helper.registerAllowedMethod("runOverNpmModules", [Map.class, Closure.class], { runOverNpmModulesCalled = true })

        def script = loadScript("vars/stageUnitTests.groovy")
        script.invokeMethod("call", [script: dummyScript])

        assertTrue(runOverNpmModulesCalled)
    }

    @Test
    void 'MTA Project should run over Modules and npm Modules'() {
        boolean runOverModulesCalled = false
        boolean runOverNpmModulesCalled = false
        BuildToolEnvironment.instance.buildTool = BuildTool.MTA
        helper.registerAllowedMethod("runOverModules", [Map.class, Closure.class], { runOverModulesCalled = true })
        helper.registerAllowedMethod("runOverNpmModules", [Map.class, Closure.class], { runOverNpmModulesCalled = true })

        def script = loadScript("vars/stageUnitTests.groovy")
        script.invokeMethod("call", [script: dummyScript])

        assertTrue(runOverModulesCalled)
        assertTrue(runOverNpmModulesCalled)
    }
}
