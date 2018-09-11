import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:]) {
    def stageName = 'artifactDeployment'
    def script = parameters.script

    runAsStage(stageName: stageName, script: script) {

        Map defaultConfig = ConfigurationLoader.defaultStageConfiguration(script, stageName)
        Map stageConfig = ConfigurationLoader.stageConfiguration(script, stageName)

        if (stageConfig.nexus) {

            //add pomPath & target folder
            Set stageConfigurationKeys = [
                'url',
                'repository',
                'version',
                'credentialsId',
                'additionalClassifiers',
                'artifactId',
                'groupId'
            ]

            Map nexusConfiguration = ConfigurationMerger.merge(stageConfig.nexus, stageConfigurationKeys, defaultConfig.nexus)

            String url = nexusConfiguration.url
            String repository = nexusConfiguration.repository
            String credentialsId = nexusConfiguration.credentialsId
            String nexusVersion = nexusConfiguration.version
            String artifactId = nexusConfiguration.artifactId
            String groupId = nexusConfiguration.groupId

            if (script.commonPipelineEnvironment.configuration.isMta) {
                def mta = readYaml file: 'mta.yaml'
                String artifactVersion = mta.version

                if (artifactId == null || artifactId.isEmpty()) {
                    artifactId = script.commonPipelineEnvironment.configuration.artifactId
                }

                print("ArtifactId is " + artifactId + " groupId is : " + groupId + " Artifact version is: " + artifactVersion)

                List artifacts = []
                artifacts.add([artifactId: artifactId,
                               classifier: '',
                               type      : 'mtar',
                               file      : script.commonPipelineEnvironment.mtarFilePath])

                artifacts.add([artifactId: artifactId,
                               classifier: '',
                               type      : 'yaml',
                               file      : 'mta.yaml'])

                def nexusUrlWithoutProtocol = url.replaceFirst("^https?://", "")

                Map nexusArtifactUploaderParameters = [nexusVersion: nexusVersion,
                                                       protocol    : 'http',
                                                       nexusUrl    : nexusUrlWithoutProtocol,
                                                       groupId     : groupId,
                                                       version     : artifactVersion,
                                                       repository  : repository,
                                                       artifacts   : artifacts]

                if (credentialsId != null) {
                    nexusArtifactUploaderParameters.put('credentialsId', credentialsId)
                }

                print("Creddentials ID " + credentialsId.toString())

                nexusArtifactUploader(nexusArtifactUploaderParameters)
            } else {
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
            }
        } else {
            error("Can't deploy to nexus because the configuration is missing. " +
                "Please ensure the `artifactDeployment` section has a `nexus` sub-section.")
        }
    }
}
