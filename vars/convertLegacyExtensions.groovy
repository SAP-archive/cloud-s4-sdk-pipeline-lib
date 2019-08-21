def call(Map parameters) {
    def script = parameters.script
    renameBackendIntegrationTests(script)
}

def renameBackendIntegrationTests(script) {
    String oldStageName = "integrationTests"
    String newStageName = "backendIntegrationTests"
    def projectInterceptorFile = "${s4SdkGlobals.projectExtensionsDirectory}/${oldStageName}.groovy"
    def repositoryInterceptorFile = "${s4SdkGlobals.repositoryExtensionsDirectory}/${oldStageName}.groovy"

    if(fileExists(projectInterceptorFile) || fileExists(repositoryInterceptorFile)) {
        error("You defined an extension for the backend integration tests stage. " +
            "However, you are using the old identifier ${oldStageName}.groovy. Please use ${newStageName}.groovy instead.")

    }
}
