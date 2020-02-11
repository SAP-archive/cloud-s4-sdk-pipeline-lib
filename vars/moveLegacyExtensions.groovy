import com.sap.cloud.sdk.s4hana.pipeline.BashUtils

import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

void call(Map parameters) {
    handleStepErrors(stepName: 'moveLegacyExtensions', stepParameters: parameters) {
        moveRepositoryExtensions()
        moveProjectExtensions()
    }
}

private void moveRepositoryExtensions() {
    String oldPath = 's4hana_pipeline/extensions'
    String newPath = s4SdkGlobals.repositoryExtensionsDirectory
    if (moveExtensions(oldPath, newPath, 'repository')) {
        addPipelineWarning("The global repository extensions need to be moved from the deprecated " +
            "location at '$oldPath' into the new location at '$newPath'. Please bring this to the " +
            "attention of the pipeline maintainer.")
    }
}

private void moveProjectExtensions(Script script ) {
    String oldPath = 'pipeline/extensions'
    String newPath = s4SdkGlobals.projectExtensionsDirectory
    if (moveExtensions(oldPath, newPath, 'project')) {
        addPipelineWarning("Please move your project extensions from the deprecated location at " +
            "'$oldPath' into the new location at '$newPath'. (Just rename the folder/path in your " +
            "project accordingly.)")
    }
}

private boolean moveExtensions(String oldPath, String newPath, String extensionsType) {
    if (oldPath == newPath || !fileExists(oldPath))
        return false

    if (fileExists(newPath)) {
        error "Your project contains two folders for ${extensionsType} extentions.\n" +
            "To resolve this situation, move or merge all extensions located from the legacy location " +
            "'${oldPath}' into the current location at '${newPath}' and delete the legacy folder."
    }

    newPath = BashUtils.escape(newPath)
    oldPath = BashUtils.escape(oldPath)

    echo "[moveLegacyExtensions] Moving old $extensionsType extensions at $oldPath into new location at $newPath."

    // Recursively create the new path, regardless of whether it already exists.
    sh "mkdir -p ${newPath}"
    // Move all files which exists at the old location into the new location. No files are expected to exists
    // at the new location, we throw an error above when the path already exists.
    sh "mv ${oldPath}/* ${newPath}"

    return true
}

private void addPipelineWarning(String message) {
    echo '[WARNING]: ' + message
    assertPluginIsActive('badge')
    addBadge(icon: "warning.gif", text: message)

    String html =
        """
<h2>Deprecated extension location</h2>
<p>$message</p>
"""

    createSummary(icon: "warning.gif", text: html)
}
