import com.sap.cloud.sdk.s4hana.pipeline.Debuglogger
import com.sap.piper.ConfigurationLoader

def call(Map parameters = [:]) {
    echo "BEGINNING TO ARCHIVE DEBUG LOG"
    try {
        def script = parameters.script

        Map postActionConfiguration = ConfigurationLoader.postActionConfiguration(script, 'archiveDebugLog')
        Debuglogger.instance.shareConfidentialInformation = postActionConfiguration?.get('shareConfidentialInformation') ?: false

        String result = Debuglogger.instance.generateReport(script)

        if (parameters.printToConsole) {
            echo result
        }

        script.writeFile file: Debuglogger.instance.fileName, text: result
        script.archiveArtifacts artifacts: Debuglogger.instance.fileName
    }
    catch (Exception e) {
        println("WARNING: The debug log was not created, it threw the following error message:")
        println("${e}")
    }
    echo "ENDING TO ARCHIVE DEBUG LOG"
}
