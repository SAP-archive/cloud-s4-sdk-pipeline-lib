void call(Map parameters) {
    def script = parameters.script
    renameBackendIntegrationTests(script)
    prohibitUnitTestsExtensions(script)
}

void renameBackendIntegrationTests(Script script) {
    String oldStageName = "integrationTests"
    String newStageName = "backendIntegrationTests"
    String projectInterceptorFile = "${s4SdkGlobals.projectExtensionsDirectory}/${oldStageName}.groovy"
    String repositoryInterceptorFile = "${s4SdkGlobals.repositoryExtensionsDirectory}/${oldStageName}.groovy"

    if (fileExists(projectInterceptorFile) || fileExists(repositoryInterceptorFile)) {
        error("You defined an extension for the backend integration tests stage. " +
            "However, you are using the old identifier ${oldStageName}.groovy. Please use ${newStageName}.groovy instead.")

    }
}

void prohibitUnitTestsExtensions(Script script) {
    String oldStageName = "stageUnitTests"
    String projectInterceptorFile = "${s4SdkGlobals.projectExtensionsDirectory}/${oldStageName}.groovy"
    String repositoryInterceptorFile = "${s4SdkGlobals.repositoryExtensionsDirectory}/${oldStageName}.groovy"

    if (fileExists(projectInterceptorFile)) {
        error("You defined an extension for $oldStageName. " +
            "This stage has been merged with the build stage and your extension will bot be run anymore. Please remove it or extend the build stage instead.")
    }

    if (fileExists(repositoryInterceptorFile)) {
        // Output only a warning for the time being, as the pipeline consumer probably has no chance to fix this on
        // his own. After some time, this should be changed into an error as well.
        echo("[WARNING] A repository extension exists for $oldStageName. " +
            "This stage has been merged with the build stage and your extension will not be run.")

    }
}
