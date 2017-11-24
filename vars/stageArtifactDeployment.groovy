import com.sap.icd.jenkins.ConfigurationLoader

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'stageArtifactDeployment', stepParameters: parameters) {
        def script = parameters.script

        Map stageConfig = ConfigurationLoader.stageConfiguration(script, 'artifactDeployment')
        if (stageConfig.nexus) {
            String url = stageConfig.nexus.url
            String repository = stageConfig.nexus.repository
            String credentialsId = stageConfig.nexus.credentialsId

            try {
                unstashFiles script: script, stage: 'deploy'

                def pom = readMavenPom file: 'pom.xml'
                String groupId = pom.groupId
                String artifactId = pom.artifactId
                String version = pom.version

                List jarFiles = findFiles(glob: 'application/target/*.jar')
                List warFiles = findFiles(glob: 'application/target/*.war')
                List earFiles = findFiles(glob: 'application/target/*.ear')

                deployArtifactsToNexus script: script, url: url, repository: repository, credentialsId: credentialsId, groupId: groupId, artifactId: artifactId, version: version, jarFiles: jarFiles, warFiles: warFiles, earFiles: earFiles
            } finally {
                stashFiles script: script, stage: 'deploy'
            }
        } else {
            println("Can't deploy to nexus because the configuration is missing.")
        }
    }
}
