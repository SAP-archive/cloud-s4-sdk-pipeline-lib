import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'sourceClearScan'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {
        def stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)
        def credentialsId = stageConfiguration.credentialsId
        def projectDefinedConfig = stageConfiguration.config
        def generalConfiguration = ConfigurationLoader.generalConfiguration(script)
        def projectName = generalConfiguration.projectName
        def scmUri = scm.getUserRemoteConfigs()[0].getUrl()
        def scmBranch = env.BRANCH_NAME
        def commitId = generalConfiguration.gitCommitId

        executeSourceClearScan script: script, credentialsId: credentialsId, projectDefinedConfig: projectDefinedConfig, projectName: projectName, scmUri: scmUri, scmBranch: scmBranch, commitId: commitId
    }
}
