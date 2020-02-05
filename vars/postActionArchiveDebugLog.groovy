import com.sap.piper.ConfigurationLoader
import com.sap.piper.DebugReport

def call(Map parameters = [:]) {
    echo "BEGINNING TO ARCHIVE DEBUG LOG"
    try {
        def script = parameters.script

        Map postActionConfiguration = ConfigurationLoader.postActionConfiguration(script, 'archiveDebugLog')
        DebugReport.instance.shareConfidentialInformation = postActionConfiguration?.get('shareConfidentialInformation') ?: false

        String result = DebugReport.instance.generateReport(script)

        if (parameters.printToConsole) {
            echo result
        }

        script.writeFile file: DebugReport.instance.fileName, text: result
        script.archiveArtifacts artifacts: DebugReport.instance.fileName
        echo "Successfully archived debug report as '${DebugReport.instance.fileName}'"
    }
    catch (Exception e) {
        println("WARNING: The debug log was not created, it threw the following error message:")
        println("${e}")
    }
    echo "ENDING TO ARCHIVE DEBUG LOG"
}
