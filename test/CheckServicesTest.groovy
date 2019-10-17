import com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils
import com.sap.cloud.sdk.s4hana.pipeline.util.BaseCloudSdkTest
import groovy.json.JsonSlurper
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

class CheckServicesTest extends BaseCloudSdkTest {

    String serviceCatalog = """{
  "d": {
    "results": [
    {
        "__metadata": {
          "id": "1",
          "uri": "test",
          "type": "content",
          "content_type": "application/octet-stream",
          "media_src": "https://sap.com",
          "edit_media": "https://sap.com"
        },
        "Name": "this_service"
      },
      {
        "__metadata": {
          "id": "1",
          "uri": "test",
          "type": "content",
          "content_type": "application/octet-stream",
          "media_src": "https://sap.com",
          "edit_media": "https://sap.com"
        },
        "Name": "that_service"
      }
    ]
  }
}"""
    def serviceCatalogJSON = new JsonSlurper().parseText(serviceCatalog)

    @Before
    void prepareTests() throws Exception {
        EnvironmentAssertionUtils.metaClass.static.assertPluginIsActive = { String pluginName -> }
        this.binding.setVariable('s4SdkGlobals', new s4SdkGlobals())
        helper.registerAllowedMethod('handleStepErrors', [Map.class, Closure.class], { Map parameters, Closure closure -> closure() })
        helper.registerAllowedMethod('fetchUrl', [String.class], { String url -> })
        helper.registerAllowedMethod('readJSON', [Map.class], { Map parameters -> return serviceCatalogJSON })

        setUp()
    }

    @Test
    void 'checkOdataServices should invoke error step when unallowed services are found'() {
        String errorMessage = ''
        helper.registerAllowedMethod('readCSV', [Map.class], { Map parameters ->
            if (parameters.file == 's4hana_pipeline/reports/service_audits/aggregated_odata_audit.log') {
                return [['testDestinationA', '/myServiceA/', 'erpUrlA', 'threadA'], ['testDestinationB', '/myServiceB/', 'erpUrlB', 'threadB']]
            }
            if (parameters.file == 's4hana_pipeline/reports/service_audits/aggregated_rfc_audit.log') {
                return []
            }
        })
        helper.registerAllowedMethod('error', [String.class], { String error -> errorMessage = error })

        def script = loadScript("vars/checkServices.groovy")
        script.invokeMethod("call", [script: dummyScript, 'nonErpDestinations': ['anyDestination'], 'customODataServices': ['anyService'], 'nonErpUrls': ['anyUrl']])

        assertEquals('FAILURE', binding.getVariable('currentBuild').result)
        assertTrue(errorMessage.contains('Your project uses non-official OData services: [myServiceA, myServiceB]'))
    }

    @Test
    void 'checkOdataServices should pass when nonErpUrls (sdk >= 3.2) and nonErpDestinations (sdk < 3.2) are configured'() {
        String errorMessage = ''
        helper.registerAllowedMethod('readCSV', [Map.class], { Map parameters ->
            if (parameters.file == 's4hana_pipeline/reports/service_audits/aggregated_odata_audit.log') {
                List csvSdkVersionTwo = ['testDestination', '/myService/', 'erpUrl', 'thread']
                List csvSdkVersionThree= ['testUrl', '/anotherService/', 'entity', 'anotherThread']
                return [csvSdkVersionTwo, csvSdkVersionThree]
            }
            if (parameters.file == 's4hana_pipeline/reports/service_audits/aggregated_rfc_audit.log') {
                return []
            }
        })
        helper.registerAllowedMethod('error', [String.class], { String error -> errorMessage = error })

        def script = loadScript("vars/checkServices.groovy")
        script.invokeMethod("call", [script: dummyScript, 'nonErpDestinations': ['testDestination'], 'customODataServices': ['testService'], 'nonErpUrls': ['testUrl']])

        assertTrue(errorMessage.isEmpty())
    }

    @Test
    void 'checkBapiServices should invoke error step when unallowed services are configured'() {
        String errorMessage = ''
        helper.registerAllowedMethod('readCSV', [Map.class], { Map parameters ->
            if (parameters.file == 's4hana_pipeline/reports/service_audits/aggregated_odata_audit.log') {
                return []
            }
            if (parameters.file == 's4hana_pipeline/reports/service_audits/aggregated_rfc_audit.log') {
                return [['typeA', 'testDestinationA', 'myServiceA', 'threadA'], ['typeB', 'testDestinationB', 'FC_ITEM_PROP_GET_RFC', 'threadB']]
            }
        })
        helper.registerAllowedMethod('error', [String.class], { String error -> errorMessage = error })

        def script = loadScript("vars/checkServices.groovy")
        script.invokeMethod("call", [script: dummyScript, 'nonErpDestinations': ['testDestination'], 'customODataServices': ['testService'], 'nonErpUrls': ['testUrl']])

        assertEquals('FAILURE', binding.getVariable('currentBuild').result)
        assertTrue(errorMessage.contains('Your project uses non-official RFC services: [myServiceA]'))
    }

    @Test
    void 'checkBapiServices should pass when nonErpDestinations are configured'() {
        String errorMessage = ''
        helper.registerAllowedMethod('readCSV', [Map.class], { Map parameters ->
            if (parameters.file == 's4hana_pipeline/reports/service_audits/aggregated_odata_audit.log') {
                return []
            }
            if (parameters.file == 's4hana_pipeline/reports/service_audits/aggregated_rfc_audit.log') {
                return [['typeA', 'testDestinationA', 'FCXL_SET_DATA', 'threadA'], ['typeB', 'testDestinationB', 'FC_ITEM_PROP_GET_RFC', 'threadB']]
            }
        })
        helper.registerAllowedMethod('error', [String.class], { String error -> errorMessage = error })

        def script = loadScript("vars/checkServices.groovy")
        script.invokeMethod("call", [script: dummyScript, 'nonErpDestinations': ['testDestination'], 'customODataServices': ['testService'], 'nonErpUrls': ['testUrl']])

        assertTrue(errorMessage.isEmpty())
    }

}
