void call(Map parameters) {
    handleStepErrors(stepName: 'checkLegacyExtensions', stepParameters: parameters) {
        prohibitOldExtensionsLocation()
        prohibitIntegrationTestExtension()
        prohibitUnitTestsExtension()
    }
}

void prohibitOldExtensionsLocation() {
    String oldPath = 'pipeline/extensions'
    if (fileExists(oldPath)) {
        error "The old location for pipeline extensions at '${oldPath}' is no longer supported.\n" +
            "To resolve this situation, move or merge all extensions located from the legacy location " +
            "'${oldPath}' into the current location at '${s4SdkGlobals.projectExtensionsDirectory}' and " +
            "delete the legacy folder."
    }
}

void prohibitIntegrationTestExtension() {
    String oldStageName = "integrationTests"
    String newStageName = "backendIntegrationTests"
    String projectInterceptorFile = "${s4SdkGlobals.projectExtensionsDirectory}/${oldStageName}.groovy"
    String repositoryInterceptorFile = "${s4SdkGlobals.repositoryExtensionsDirectory}/${oldStageName}.groovy"

    if (fileExists(projectInterceptorFile) || fileExists(repositoryInterceptorFile)) {
        error("You defined an extension for the backend integration tests stage. " +
            "However, you are using the old identifier ${oldStageName}.groovy. Please use ${newStageName}.groovy instead.")

    }
}

void prohibitUnitTestsExtension() {
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
