import com.sap.cloud.sdk.s4hana.pipeline.Analytics
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration

def call(Map parameters = [:]) {
    Script script = parameters.script

    // If you change the version please also update the version in build.gradle. They must always be at the same commit/tag/version.
    String piperOsVersion = 'v1.46.0'

    String piperIdentifier = 'None'

    if(isLibraryConfigured("piper-lib-os")){
        piperIdentifier = "piper-lib-os"
    }
    else if(isLibraryConfigured("piper-library-os")){
        error("You are using the old global library identifier piper-library-os. Please change the name to piper-lib-os or make sure to setup Jenkins with the latest Docker image as described here: https://github.com/SAP/devops-docker-cx-server/blob/master/docs/operations/cx-server-operations-guide.md")
    }
    else {
        error("Configuration missing for required libraries. Please setup Jenkins as described here: https://github.com/SAP/devops-docker-cx-server/blob/master/docs/operations/cx-server-operations-guide.md")
    }

    library "${piperIdentifier}@${piperOsVersion}"
    Analytics.instance.setPiperIdentifier(piperIdentifier)

    unstashPiperBinInNonReleaseVersions(piperVersion: piperOsVersion)
}

private boolean isLibraryConfigured(String libName){
    GlobalLibraries globalLibraries = GlobalLibraries.get()
    List libs = globalLibraries.getLibraries()

    for (LibraryConfiguration libConfig : libs) {
        if (libConfig.getName() == libName) {
            return true
        }
    }

    return false
}
