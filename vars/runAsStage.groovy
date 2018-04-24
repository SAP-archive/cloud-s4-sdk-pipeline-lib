import com.sap.cloud.sdk.s4hana.pipeline.ClosureHolder
import com.sap.piper.ConfigurationHelper
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:], body) {
    ConfigurationHelper configurationHelper = new ConfigurationHelper(parameters)
    def stageName = configurationHelper.getMandatoryProperty('stageName')
    def script = configurationHelper.getMandatoryProperty('script')

    Map defaultGeneralConfiguration = ConfigurationLoader.defaultGeneralConfiguration(script)
    Map projectGeneralConfiguration = ConfigurationLoader.generalConfiguration(script)

    Map generalConfiguration = ConfigurationMerger.merge(
        projectGeneralConfiguration,
        projectGeneralConfiguration.keySet(),
        defaultGeneralConfiguration
    )

    Map stageDefaultConfiguration = ConfigurationLoader.defaultStageConfiguration(script, stageName)
    Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)

    Set parameterKeys = ['node']
    Map mergedStageConfiguration = ConfigurationMerger.merge(
        parameters,
        parameterKeys,
        stageConfiguration,
        stageConfiguration.keySet(),
        stageDefaultConfiguration
    )

    def nodeLabel = generalConfiguration.defaultNode

    if (mergedStageConfiguration.node) {
        nodeLabel = mergedStageConfiguration.node
    }

    handleStepErrors(stepName: stageName, stepParameters: [:]) {
        node(nodeLabel) {
            try {
                unstashFiles script: script, stage: stageName
                executeStage(stageName, mergedStageConfiguration, generalConfiguration, body)
                stashFiles script: script, stage: stageName
                echo "Current build result in stage $stageName is ${script.currentBuild.result}."
            }
            finally {
                deleteDir()
            }
        }
    }
}

private executeStage(stageName, stageConfiguration, generalConfiguration, body) {
    def stageInterceptor = "pipeline/extensions/${stageName}.groovy"
    if (fileExists(stageInterceptor)) {
        Script interceptor = load(stageInterceptor)
        interceptor.binding.setProperty('originalStage', new ClosureHolder(body as Closure))
        interceptor.binding.setProperty('stageName', stageName)
        interceptor.binding.setProperty('stageConfiguration', stageConfiguration)
        interceptor.binding.setProperty('generalConfiguration', generalConfiguration)
        echo "Running interceptor for ${stageName}."
        interceptor()
    } else {
        body()
    }
}
