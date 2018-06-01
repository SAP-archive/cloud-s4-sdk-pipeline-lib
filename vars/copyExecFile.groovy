def call(Map parameters) {
    sh "mkdir -p ${s4SdkGlobals.coverageReports}"

    for (int x = 0; x < parameters.execFiles.size(); x++) {
        String execFile = parameters.execFiles[x]
        echo "Testing ${execFile}"
        if (fileExists(execFile)) {
            echo "copying ${execFile}"
            sh "cp ${execFile} ${s4SdkGlobals.coverageReports}/${parameters.target}"
            return
        }
    }
    echo "Warning: No code coverage file found."
}
