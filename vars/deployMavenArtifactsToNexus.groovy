import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'deployMavenArtifactsToNexus', stepParameters: parameters) {

        def script = parameters.script
        def defaultConfiguration = ConfigurationLoader.defaultStepConfiguration(script, 'deployMavenArtifactsToNexus')

        Set parameterKeys = [
            'url',
            'repository',
            'nexusVersion',
            'credentialsId',
            'pomPath',
            'targetFolder',
            'additionalClassifiers'
        ]

        def configuration = ConfigurationMerger.merge(parameters, parameterKeys, defaultConfiguration)

        def pomFile = configuration.pomPath ? "${configuration.pomPath}/pom.xml" : "pom.xml"
        def pom = readPom(script, configuration, pomFile)

        List artifacts = []
        artifacts.add([artifactId: pom.artifactId,
                       classifier: '',
                       type      : 'pom',
                       file      : pomFile])

        if (pom.packaging != 'pom') {
            def packaging = pom.packaging ?: 'jar'
            String finalName = pom.getBuild().getFinalName()
            artifacts.add([artifactId: pom.artifactId,
                           classifier: '',
                           type      : packaging,
                           file      : "${configuration.targetFolder}/${finalName}.$packaging"])
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
                                               groupId     : pom.groupId,
                                               version     : pom.version,
                                               repository  : configuration.repository,
                                               artifacts   : artifacts]

        if (configuration.credentialsId != null) {
            nexusArtifactUploaderParameters.put('credentialsId', configuration.credentialsId)
        }

        nexusArtifactUploader(nexusArtifactUploaderParameters)
    }
}

def generateEffectivePom(script, pomFile, configuration) {
    mavenExecute(
        script: script,
        flags: '--batch-mode',
        pomPath: "$pomFile",
        m2Path: s4SdkGlobals.m2Directory,
        goals: 'help:effective-pom',
        dockerImage: configuration.dockerImage,
        defines: "-Doutput=effectivePom.xml"
    )

    return configuration.pomPath ? "${configuration.pomPath}/effectivePom.xml" : "effectivePom.xml"
}

def readPom(script, configuration, pomFile) {
    def pom
    if (configuration.pomPath) {
        def effectivePomPath = generateEffectivePom(script, pomFile, configuration)
        pom = readMavenPom file: effectivePomPath
    } else {
        pom = readMavenPom file: pomFile
    }
    return pom
}
