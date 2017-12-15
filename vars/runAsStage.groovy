import com.sap.icd.jenkins.ConfigurationHelper
import com.sap.icd.jenkins.ConfigurationLoader
import com.sap.icd.jenkins.ConfigurationMerger

def call(Map parameters = [:], body) {
    ConfigurationHelper configurationHelper = new ConfigurationHelper(parameters)
    def stageName = configurationHelper.getMandatoryProperty('stageName')
    def script = configurationHelper.getMandatoryProperty('script')

    Map defaultGeneralConfiguration = ConfigurationLoader.defaultGeneralConfiguration(script)
    Map projectGeneralConfiguration = ConfigurationLoader.generalConfiguration(script)
    def generalConfigurationKeys = ['defaultNode']
    Map generalConfiguration = ConfigurationMerger.merge(projectGeneralConfiguration, generalConfigurationKeys, defaultGeneralConfiguration)

    Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)


    def nodeLabel = generalConfiguration.defaultNode

    if(stageConfiguration.node){
        nodeLabel = stageConfiguration.node
    }

    handleStepErrors(stepName: stageName, stepParameters: [:]) {
        node(nodeLabel) {
            body()
        }
    }
}