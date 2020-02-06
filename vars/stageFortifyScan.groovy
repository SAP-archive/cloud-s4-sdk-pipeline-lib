import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.QualityCheck
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'fortifyScan'
    def script = parameters.script
    def stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)
    piperStageWrapper(stageName: stageName, script: script) {
        runOverModules(script: script, moduleType: 'java') { String basePath ->
            executeFortifyScan(script: script,
                fortifyCredentialId: stageConfiguration.fortifyCredentialId,
                fortifyProjectName: stageConfiguration.fortifyProjectName,
                projectVersionId: stageConfiguration.projectVersionId,
                sscUrl: stageConfiguration.sscUrl,
                basePath: BuildToolEnvironment.instance.getApplicationPath(basePath)
            )
        }
        ReportAggregator.instance.reportVulnerabilityScanExecution(QualityCheck.FortifyScan)
    }
}

