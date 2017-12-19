import com.sap.icd.jenkins.ConfigurationLoader

def call(Map parameters = [:]) {
    def script = parameters.script

    runAsStage(stageName: 'artifactDeployment', script: script) {
        Map stageConfig = ConfigurationLoader.stageConfiguration(script, 'artifactDeployment')
        if (stageConfig.nexus) {
            String url = stageConfig.nexus.url
            String repository = stageConfig.nexus.repository
            String credentialsId = stageConfig.nexus.credentialsId
            String nexusVersion = stageConfig.nexus.version

            try {
                unstashFiles script: script, stage: 'deploy'

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
                        targetFolder: 'application/target')

            } finally {
                stashFiles script: script, stage: 'deploy'
            }
        } else {
            println("Can't deploy to nexus because the configuration is missing.")
        }
    }
}
