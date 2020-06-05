import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters) {
    def script = parameters.script
    def stageName = parameters.stageName
    def stageConfigurationKeys = parameters.stageConfigurationKeys

    Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)
    Map defaultStageConfiguration = ConfigurationLoader.defaultStageConfiguration(script, stageName)
    Map configWithDefault = ConfigurationMerger.merge(stageConfiguration, stageConfigurationKeys, defaultStageConfiguration)

    return configWithDefault
}

