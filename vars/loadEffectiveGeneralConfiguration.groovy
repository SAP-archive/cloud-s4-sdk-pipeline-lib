import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters) {
    def script = parameters.script

    Map generalConfiguration = ConfigurationLoader.generalConfiguration(script)
    Map defaultGeneralConfiguration = ConfigurationLoader.defaultGeneralConfiguration(script)
    Map configWithDefault = ConfigurationMerger.merge(generalConfiguration, generalConfiguration.keySet(), defaultGeneralConfiguration)

    return configWithDefault
}
