import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationHelper
import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationLoader
import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationMerger

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
            try {
                unstashFiles script: script, stage: stageName
                body()
                stashFiles script: script, stage: stageName
                echo "Current build result in stage $stageName is ${script.currentBuild.result}"
            }
            finally {
                deleteDir()
            }
        }
    }
}