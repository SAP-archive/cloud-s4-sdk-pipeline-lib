import hudson.model.Result
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration

def call(Map parameters = [:]) {
    Script script = parameters.script

    // If you change the version please also update build.gradle and the corresponding jar file
    String piperOsVersion = '9f4a597778696a49afedd1908b2d91ea0608ceee'

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
    trackPiperIdentifier(script, piperIdentifier)
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

private trackPiperIdentifier(Script script, String piperIdentifier){
    def piperInfo = [:]
    piperInfo.event_type = 'load_piper'

    piperInfo.custom3 = 'piper_identifier'
    piperInfo.e_3 = piperIdentifier

    sendAnalytics(script: script, telemetryData: piperInfo)
}
