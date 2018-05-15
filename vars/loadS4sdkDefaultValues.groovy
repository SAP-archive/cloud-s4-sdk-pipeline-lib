import com.sap.piper.ConfigurationMerger
import com.sap.piper.DefaultValueCache

def call(Map parameters = [:]) {

    prepareDefaultValues script: parameters.script
    Map currentDefaultValues = DefaultValueCache.getInstance().getDefaultValues()
    Map s4sdkDefaultValues = readYaml text: libraryResource('default_s4_pipeline_environment.yml')

    Map mergedConfiguration = ConfigurationMerger.merge(
        s4sdkDefaultValues,
        s4sdkDefaultValues.keySet(),
        currentDefaultValues
    )

    DefaultValueCache.createInstance(mergedConfiguration)
}
