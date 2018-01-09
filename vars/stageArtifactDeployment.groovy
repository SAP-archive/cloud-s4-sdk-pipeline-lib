import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationLoader
import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationMerger

def call(Map parameters = [:]) {
    def stageName = 'artifactDeployment'
    def script = parameters.script

    runAsStage(stageName: stageName, script: script) {

        Map defaultConfig = ConfigurationLoader.defaultStageConfiguration(script, stageName)
        Map stageConfig = ConfigurationLoader.stageConfiguration(script, stageName)

        if (stageConfig.nexus) {

            List stageConfigurationKeys = [
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

            def pom = readMavenPom file: 'pom.xml'

            deployMavenArtifactsToNexus(
                    script: script,
                    url: url,
                    nexusVersion: nexusVersion,
                    repository: repository,
                    credentialsId: credentialsId,
                    pomFile: 'pom.xml',
                    targetFolder: 'target')

            deployMavenArtifactsToNexus(
                    script: script,
                    url: url,
                    nexusVersion: nexusVersion,
                    repository: repository,
                    credentialsId: credentialsId,
                    pomFile: 'application/pom.xml',
                    targetFolder: 'application/target',
                    additionalClassifiers: nexusConfiguration.additionalClassifiers,
                    defaultGroupId: pom.groupId)

        } else {
            println("Can't deploy to nexus because the configuration is missing.")
        }
    }
}
