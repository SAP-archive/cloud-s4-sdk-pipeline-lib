import com.sap.cloud.sdk.s4hana.pipeline.WarningsUtils

String call(Map parameters) {
    Script script = parameters.script
    String oldPath = 'pipeline_config.yml'
    String newFolder = ".pipeline"
    String newPath = "$newFolder/config.yml"

    if (!fileExists(oldPath)) {
        return newPath;
    }

    if (fileExists(newPath)) {
        error "Your project contains two pipeline configuration files.\n" +
            "To resolve this situation, migrate the configuration located from " +
            "'${oldPath}' into the current location at '${newPath}' and delete the old file."
    }

    WarningsUtils.addPipelineWarning(
        script,
        "Deprecated configuration location",
        "The configuration file needs to be moved from the deprecated " +
        "location at '$oldPath' into the new location at '$newPath'. " +
        "Please bring this to the attention of the pipeline maintainer."
    )

    return oldPath
}
