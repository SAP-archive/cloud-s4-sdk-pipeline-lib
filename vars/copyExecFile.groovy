def call(Map parameters) {
    sh "mkdir -p ${s4SdkGlobals.coverageReports}"

    String targetFolder = parameters.targetFolder ?: './'
    String targetFile = parameters.targetFile

    for (int x = 0; x < parameters.execFiles.size(); x++) {
        String execFile = parameters.execFiles[x]
        echo "Testing ${execFile}"
        if (fileExists(execFile)) {
            echo "copying ${execFile}"
            sh "mkdir -p ${s4SdkGlobals.coverageReports}/${targetFolder}"
            sh "cp ${execFile} ${s4SdkGlobals.coverageReports}/${targetFolder}/${targetFile}"
            return
        }
    }
    echo "Warning: No code coverage file found."
}
