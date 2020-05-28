import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters) {
    def script = parameters.script
    def stepName = parameters.stepName
    def stepConfigKeys = parameters.stepConfigKeys

    Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, stepName)
    Map defaultStepConfiguration = ConfigurationLoader.defaultStepConfiguration(script, stepName)
    Map configWithDefault = ConfigurationMerger.merge(stepConfiguration, stepConfigKeys, defaultStepConfiguration)

    return configWithDefault
}
