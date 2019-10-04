import com.sap.cloud.sdk.s4hana.pipeline.Debuglogger

def call(Map parameters) {
    def script = parameters.script

    def sharedProjectLibrariesFile = "${s4SdkGlobals.projectExtensionsDirectory}/sharedLibraries.yml"
    def sharedRepositoryLibrariesFile = "${s4SdkGlobals.repositoryExtensionsDirectory}/sharedLibraries.yml"

    loadLibrariesFromFile(sharedProjectLibrariesFile, "local")
    loadLibrariesFromFile(sharedRepositoryLibrariesFile, "global")
}

private loadLibrariesFromFile(String filename, String loadedByExtension) {
    if (fileExists(filename)) {
        List libs = readYaml file: filename
        Set additionalLibraries = []
        for (i = 0; i < libs.size(); i++) {
            Map lib = libs[i]
            String libName = libs[i].name
            String branch = libs[i].version ?: 'master'
            // FIXME: Check if library was already loaded (maybe via Jenkins API?)
            additionalLibraries.add("${libName} | ${branch} | ${loadedByExtension}")
            library "${libName}@${branch}"
        }
        Debuglogger.instance.additionalSharedLibraries.addAll(additionalLibraries)
    }
}
