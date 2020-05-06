import com.sap.cloud.sdk.s4hana.pipeline.QualityCheck
import com.sap.cloud.sdk.s4hana.pipeline.ReportAggregator
import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'sourceClearScan'
    def script = parameters.script
    piperStageWrapper(stageName: stageName, script: script) {
        def stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)
        def credentialsId = stageConfiguration.credentialsId
        def projectDefinedConfig = stageConfiguration.config
        def generalConfiguration = ConfigurationLoader.generalConfiguration(script)
        def projectName = generalConfiguration.projectName
        def scmUri = scm.getUserRemoteConfigs()[0].getUrl()
        def scmBranch = env.BRANCH_NAME
        def commitId = script.commonPipelineEnvironment.gitCommitId

        executeSourceClearScan script: script, credentialsId: credentialsId, projectDefinedConfig: projectDefinedConfig, projectName: projectName, scmUri: scmUri, scmBranch: scmBranch, commitId: commitId

        ReportAggregator.instance.reportVulnerabilityScanExecution(QualityCheck.SourceClearScan)
    }
}
