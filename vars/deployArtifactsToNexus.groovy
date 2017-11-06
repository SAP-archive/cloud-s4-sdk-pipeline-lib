def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'deployArtifactsToNexus', stepParameters: parameters) {

        List artifacts = []
        artifacts.addAll(parameters.jarFiles.collect {
            [artifactId: parameters.artifactId,
             classifier: '',
             file      : it.path,
             type      : 'jar']
        })
        artifacts.addAll(parameters.warFiles.collect {
            [artifactId: parameters.artifactId,
             classifier: '',
             file      : it.path,
             type      : 'war']
        })
        artifacts.addAll(parameters.earFiles.collect {
            [artifactId: parameters.artifactId,
             classifier: '',
             file      : it.path,
             type      : 'ear']
        })

        Map nexusArtifactUploaderParameters = [nexusVersion: 'nexus3',
                                               protocol    : 'http',
                                               nexusUrl    : parameters.url,
                                               groupId     : parameters.groupId,
                                               version     : parameters.version,
                                               repository  : parameters.repository,
                                               artifacts   : artifacts]

        if (parameters.credentialsId != null) {
            nexusArtifactUploaderParameters.put('credentialsId', parameters.credentialsId)
        }

        nexusArtifactUploader(nexusArtifactUploaderParameters)
    }
}
