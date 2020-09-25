import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils

import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

// keep because it is used by legacy consumers

def call(Map parameters){
    Script script = parameters.script
    List customScripts = parameters.customScripts ?: []
    npmExecuteScripts script: script, runScripts: customScripts, install: true
}

