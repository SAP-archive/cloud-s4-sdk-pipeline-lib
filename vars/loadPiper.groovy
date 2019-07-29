import com.sap.cloud.sdk.s4hana.pipeline.Analytics
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration

def call(Map parameters = [:]) {
    Script script = parameters.script

    // If you change the version please also update build.gradle and the corresponding jar file
    String piperOsVersion = '7845e18f4d9629f382fdf5ceb92885281ed70e46'

    String piperIdentifier = 'None'

    if(isLibraryConfigured("piper-library-os")){
        piperIdentifier = "piper-library-os"
    }
    else if(isLibraryConfigured("piper-lib-os")){
        piperIdentifier = "piper-lib-os"
    }
    else {
        error("Configuration missing for required libraries. Please setup Jenkins as described here: https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/README.md")
    }

    library "${piperIdentifier}@${piperOsVersion}"
    Analytics.instance.setPiperIdentifier(piperIdentifier)
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
