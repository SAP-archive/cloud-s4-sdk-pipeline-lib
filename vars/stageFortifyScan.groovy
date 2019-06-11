import com.sap.cloud.sdk.s4hana.pipeline.QualityCheck
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationLoader
import com.sap.cloud.sdk.s4hana.pipeline.PathUtils

def call(Map parameters = [:]) {
    def stageName = 'fortifyScan'
    def script = parameters.script
    def stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)
    runAsStage(stageName: stageName, script: script) {
        runOverModules(script: script, moduleType: 'java') { basePath ->
            executeFortifyScan(
                script: script,
                fortifyCredentialId: stageConfiguration.fortifyCredentialId,
                fortifyProjectName: stageConfiguration.fortifyProjectName,
                projectVersionId: stageConfiguration.projectVersionId,
                sscUrl: stageConfiguration.sscUrl,
                basePath: PathUtils.normalize(basePath, 'application')
            )
        }
        ReportAggregator.instance.reportVulnerabilityScanExecution(QualityCheck.FortifyScan)
    }
}

