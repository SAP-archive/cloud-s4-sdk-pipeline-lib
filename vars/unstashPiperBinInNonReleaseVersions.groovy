import com.sap.piper.PiperGoUtils

/*
Do one initial unstash of the go binary in non-release cases to allow testing with unreleased versions
 */
def call(Map parameters) {
    def piperVersion = parameters.piperVersion
    if (!(piperVersion.matches("^v[0-9.]+") || piperVersion == 'master')) {
        withEnv(['REPOSITORY_UNDER_TEST=SAP/jenkins-library', 'LIBRARY_VERSION_UNDER_TEST=' + piperVersion]) {
            new PiperGoUtils(this).unstashPiperBin()
        }
        sh './piper version'
    }
}
