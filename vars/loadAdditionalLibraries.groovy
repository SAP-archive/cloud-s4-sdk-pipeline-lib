def call(Map parameters) {
    def script = parameters.script

    def sharedProjectLibrariesFile = "${s4SdkGlobals.projectExtensionsDirectory}/sharedLibraries.yml"
    def sharedRepositoryLibrariesFile = "${s4SdkGlobals.repositoryExtensionsDirectory}/sharedLibraries.yml"

    loadLibrariesFromFile(sharedProjectLibrariesFile)
    loadLibrariesFromFile(sharedRepositoryLibrariesFile)
}

private loadLibrariesFromFile(String filename) {
    if (fileExists(filename)) {
        List libs = readYaml file: filename

        for(i=0; i<libs.size(); i++){
            Map lib = libs[i]
            String libName = libs[i].name
            String branch = libs[i].version?:'master'
            // FIXME: Check if library was already loaded (maybe via Jenkins API?)
            library "${libName}@${branch}"
        }

    }
}
