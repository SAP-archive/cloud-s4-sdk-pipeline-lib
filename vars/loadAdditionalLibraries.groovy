def call(Map parameters) {
    def script = parameters.script

    def sharedLibrariesFile = "pipeline/extensions/sharedLibraries.yml"
    if (fileExists(sharedLibrariesFile)) {
        List libs = readYaml file: sharedLibrariesFile

        for(i=0; i<libs.size(); i++){
            Map lib = libs[i]
            String libName = libs[i].name
            String branch = libs[i].version?:'master'
            library "${libName}@${branch}"
        }

    }
}
