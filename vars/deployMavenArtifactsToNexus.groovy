import com.sap.icd.jenkins.ConfigurationLoader
import com.sap.icd.jenkins.ConfigurationMerger

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'deployMavenArtifactsToNexus', stepParameters: parameters) {

        def script = parameters.script
        def defaultConfiguration = ConfigurationLoader.defaultStepConfiguration(script, 'deployMavenArtifactsToNexus')

        def parameterKeys = [
            'nexusVersion',
            'url',
            'repository',
            'credentialsId',
            'pomFile',
            'targetFolder',
            'defaultGroupId'
        ]

        def configuration = ConfigurationMerger.merge(parameters, parameterKeys, defaultConfiguration)

        def pom = readMavenPom file: configuration.pomFile

        List artifacts = []

        def groupId = pom.groupId ?: configuration.defaultGroupId

        artifacts.add([artifactId: pom.artifactId,
                       classifier: '',
                       type: 'pom',
                       file: configuration.pomFile])

        if(pom.packaging != 'pom'){
            artifacts.add([artifactId: pom.artifactId,
                           classifier: '',
                           type: pom.packaging,
                           file: "${configuration.targetFolder}/${pom.artifactId}.${pom.packaging}"])
        }

        Map nexusArtifactUploaderParameters = [nexusVersion: configuration.nexusVersion,
                                               protocol    : 'http',
                                               nexusUrl    : configuration.url,
                                               groupId     : groupId,
                                               version     : pom.version,
                                               repository  : configuration.repository,
                                               artifacts   : artifacts]

        if (parameters.credentialsId != null) {
            nexusArtifactUploaderParameters.put('credentialsId', parameters.credentialsId)
        }

        nexusArtifactUploader(nexusArtifactUploaderParameters)
    }
}
