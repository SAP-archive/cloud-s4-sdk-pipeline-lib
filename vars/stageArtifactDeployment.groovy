import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:]) {
    def stageName = 'artifactDeployment'
    def script = parameters.script

    runAsStage(stageName: stageName, script: script) {

        Map defaultConfig = ConfigurationLoader.defaultStageConfiguration(script, stageName)
        Map stageConfig = ConfigurationLoader.stageConfiguration(script, stageName)

        if (stageConfig.nexus) {

            Set stageConfigurationKeys = [
                'url',
                'repository',
                'version',
                'credentialsId',
                'additionalClassifiers'
            ]

            Map nexusConfiguration = ConfigurationMerger.merge(stageConfig.nexus, stageConfigurationKeys, defaultConfig.nexus)

            String url = nexusConfiguration.url
            String repository = nexusConfiguration.repository
            String credentialsId = nexusConfiguration.credentialsId
            String nexusVersion = nexusConfiguration.version

            deployMavenArtifactsToNexus(
                script: script,
                url: url,
                nexusVersion: nexusVersion,
                repository: repository,
                credentialsId: credentialsId,
                pomPath: '',
                targetFolder: 'target'
            )

            deployMavenArtifactsToNexus(
                script: script,
                url: url,
                nexusVersion: nexusVersion,
                repository: repository,
                credentialsId: credentialsId,
                pomPath: 'application',
                targetFolder: 'application/target',
                additionalClassifiers: nexusConfiguration.additionalClassifiers
            )

        } else {
            println("Can't deploy to nexus because the configuration is missing.")
        }
    }
}
