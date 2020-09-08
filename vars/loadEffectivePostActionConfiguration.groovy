import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger
import com.sap.piper.DefaultValueCache

def call(Map parameters) {
    def script = parameters.script
    def postAction = parameters.postAction
    def postActionConfigKeys = parameters.postActionConfigKeys

    Map postActionConfiguration = ConfigurationLoader.postActionConfiguration(script, postAction)
    Map postActionDefaultConfiguration = DefaultValueCache.getInstance()?.getDefaultValues()?.get('postActions')?.get(postAction) ?: [:]
    Map configWithDefault = ConfigurationMerger.merge(postActionConfiguration, postActionConfigKeys, postActionDefaultConfiguration)

    return configWithDefault
}
