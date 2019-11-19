import com.sap.cloud.sdk.s4hana.pipeline.BuildTool
import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.cloud.sdk.s4hana.pipeline.util.BaseCloudSdkTest
import com.sap.piper.DefaultValueCache
import org.junit.Before
import org.junit.Test
import static org.junit.Assert.assertEquals

class CheckCodeCoverageTest extends BaseCloudSdkTest {

    @Before
    void prepareTests() throws Exception {

        setUp()

        this.binding.setVariable('s4SdkGlobals', new s4SdkGlobals())
        helper.registerAllowedMethod('handleStepErrors', [Map.class, Closure.class], { Map parameters, Closure closure ->
            closure.call()

        })
        helper.registerAllowedMethod('executeWithLockedCurrentBuildResult', [Map.class, Closure.class], { Map parameters, Closure closure ->
            closure.call()
        })
        helper.registerAllowedMethod('runOverModules', [Map.class, Closure.class], { Map parameters, Closure closure ->
        })
        helper.registerAllowedMethod('findFiles', [Map.class], { Map parameters -> []

        })
        DefaultValueCache.createInstance([:])
        BuildToolEnvironment.instance.buildTool = BuildTool.MAVEN
        ReportAggregator.metaClass.static.reportCodeCoverageCheck = { Script script, String unstableCodeCoverage, List jacocoExcludes -> }
        EnvironmentAssertionUtils.metaClass.static.assertPluginIsActive = { String pluginName -> }
        dummyScript.buildFailureReason = [:]
        dummyScript.buildFailureReason.setFailureReason = {}
    }

    @Test
    void 'Verify user provided thresholds should not be less than default thresholds '() {

        int successCoverage, unstableCoverage
        int userProvidedSuccessCoverage = 65
        int userProvidedUnstableCoverage = 10
        int defaultSuccessCoverage = 70
        int defaultUnstableCoverage = 65

        Script script = loadScript("vars/checkCodeCoverage.groovy")

        helper.registerAllowedMethod('jacoco', [Map.class], { Map parameters ->
            successCoverage = Integer.parseInt(parameters.maximumLineCoverage)
            unstableCoverage = Integer.parseInt(parameters.minimumLineCoverage)
        })

        script.invokeMethod("call", [script              : dummyScript,
                                     jacocoExcludes      : ['com/sap/cloud/s4hana/examples/addressmgr/custom/**'],
                                     codeCoverageFrontend: [codeCoverageFrontend: 'codeCoverageFrontend'],
                                     threshold           : [successCoverage: userProvidedSuccessCoverage, unstableCoverage: userProvidedUnstableCoverage]
        ])

        assertEquals(defaultSuccessCoverage, successCoverage)
        assertEquals(defaultUnstableCoverage, unstableCoverage)
    }

    @Test
    void 'Verify user provided thresholds should be equal to or greater than default thresholds'() {

        int successCoverage, unstableCoverage
        int userProvidedUnstableCoverage = 69, userProvidedSuccessCoverage = 80

        Script script = loadScript("vars/checkCodeCoverage.groovy")

        helper.registerAllowedMethod('jacoco', [Map.class], { Map parameters ->
            successCoverage = Integer.parseInt(parameters.maximumLineCoverage)
            unstableCoverage = Integer.parseInt(parameters.minimumLineCoverage)
        })

        script.invokeMethod("call", [script              : dummyScript,
                                     jacocoExcludes      : ['com/sap/cloud/s4hana/examples/addressmgr/custom/**'],
                                     codeCoverageFrontend: [codeCoverageFrontend: 'codeCoverageFrontend'],
                                     threshold           : [successCoverage: userProvidedSuccessCoverage, unstableCoverage: userProvidedUnstableCoverage]
        ])

        assertEquals(unstableCoverage, unstableCoverage)
        assertEquals(successCoverage, successCoverage)
    }

}
