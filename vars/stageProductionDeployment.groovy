import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationLoader
import com.sap.cloud.sdk.s4hana.pipeline.EndToEndTestType
import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment

def call(Map parameters = [:]) {
    def stageName = 'productionDeployment'
    def script = parameters.script

    def commonPipelineEnvironment = script.commonPipelineEnvironment
    List unstableSteps = commonPipelineEnvironment?.getValue('unstableSteps') ?: []
    if (unstableSteps) {
        piperPipelineStageConfirm script: script
        unstableSteps = []
        commonPipelineEnvironment.setValue('unstableSteps', unstableSteps)
    }

    piperStageWrapper(stageName: stageName, script: script) {
        Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)

        final def tmsStepDefaults = ConfigurationLoader.defaultStepConfiguration(script, 'tmsUpload')

        lock(script.commonPipelineEnvironment.configuration.productionDeploymentLock) {
            //other milestones are defined in the pipeline
            milestone 80
            if (stageConfiguration.tmsUpload && BuildToolEnvironment.instance.isMta()) {
                tmsUpload(
                    script: script,
                    mtaPath: script.commonPipelineEnvironment.mtarFilePath,
                    nodeName: stageConfiguration.tmsUpload.nodeName,
                    credentialsId: stageConfiguration.tmsUpload.credentialsId,
                    customDescription: stageConfiguration.tmsUpload.customDescription ?: "",
                    namedUser: tmsStepDefaults.namedUser,
                    stashContent: []
                )
            }
            if(stageConfiguration.cfTargets || stageConfiguration.neoTargets){
                if (fileExists('package.json') && stageConfiguration.appUrls) {
                    try {
                        deployToCloudPlatform(
                            script: script,
                            cfTargets: stageConfiguration.cfTargets,
                            neoTargets: stageConfiguration.neoTargets,
                            isProduction: true,
                            stage: stageName
                        )
                    }
                    finally {
                        executeEndToEndTest(
                            script: script,
                            appUrls: stageConfiguration.appUrls,
                            endToEndTestType: EndToEndTestType.SMOKE_TEST,
                            stage: stageName
                        )
                    }
                } else {
                    deployToCloudPlatform script: script, cfTargets: stageConfiguration.cfTargets, neoTargets: stageConfiguration.neoTargets, isProduction: true, stage: stageName
                    echo "Smoke tests skipped, because package.json does not exist or stage configuration option appUrls is not defined."
                }
                ReportAggregator.instance.reportDeployment()
            }
        }
    }
}
