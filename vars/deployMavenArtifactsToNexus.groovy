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
                'defaultGroupId',
                'additionalClassifiers'
        ]

        def configuration = ConfigurationMerger.merge(parameters, parameterKeys, defaultConfiguration)

        def pom = readMavenPom file: configuration.pomFile

        List artifacts = []

        def groupId = pom.groupId ?: configuration.defaultGroupId

        artifacts.add([artifactId: pom.artifactId,
                       classifier: '',
                       type      : 'pom',
                       file      : configuration.pomFile])

        if (pom.packaging != 'pom') {
            def packaging = pom.packaging ?: 'jar'
            artifacts.add([artifactId: pom.artifactId,
                           classifier: '',
                           type      : packaging,
                           file      : "${configuration.targetFolder}/${pom.artifactId}.$packaging"])
        }

        if (configuration.additionalClassifiers) {
            for (def i = 0; i < configuration.additionalClassifiers.size(); i++) {
                def additionalClassifier = configuration.additionalClassifiers[i]

                artifacts.add([artifactId: pom.artifactId,
                               classifier: additionalClassifier.classifier,
                               type      : additionalClassifier.type,
                               file      : "${configuration.targetFolder}/${pom.artifactId}-${additionalClassifier.classifier}.${additionalClassifier.type}"])
            }
        }

        def nexusUrlWithoutProtocol = configuration.url.replaceFirst("^https?://", "")

        Map nexusArtifactUploaderParameters = [nexusVersion: configuration.nexusVersion,
                                               protocol    : 'http',
                                               nexusUrl    : nexusUrlWithoutProtocol,
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
