import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonBuilder


def call(Map parameters) {
    Script script = parameters.script
    def configFileContent = readYaml file: getConfigLocation(script: script)
    def configFileAsJson = new JsonBuilder(configFileContent).toPrettyString()
    writeFile file: 'pipeline_config.json', text: configFileAsJson

    int status = 0
    if (!fileExists("./cloud-sdk-pipeline-config-schema.json")){
        status = sh(script: "curl --location --remote-name \"https://raw.githubusercontent.com/SchemaStore/schemastore/master/src/schemas/json/cloud-sdk-pipeline-config-schema.json\"", returnStatus: true)
    }

    if (status == 0) {
        executeNpm(script: script, defaultNpmRegistry: 'https://registry.npmjs.org/') {
            // In case the npx ajv-cli validate command below finds schema violations, the output is written to a single
            // line in JSON format, because of using the --errors=line cli option. Furthermore, the output is written to
            // stderr by default.
            status = sh(script: "npx ajv-cli validate --errors=line -s 'cloud-sdk-pipeline-config-schema.json' -d 'pipeline_config.json' --all-errors 2> output.json", returnStatus: true)
            if (status == 1) {
                String ajvOutput = readFile file: './output.json'
                List ajvOutputList = ajvOutput.split('\n')
                // Eliminate unnecessary lines of the output produced by ajv-cli. The JSON output usually starts with
                // '[{' in line 3 of the output.
                for (int i = 0; i < ajvOutputList.size(); i++) {
                    if (ajvOutputList.get(i).startsWith('[{')) {
                        def results = readJSON text: ajvOutputList.get(i)
                        generateWarnings(results)
                        break
                    }
                }
            }
        }
    }
    else {
        println("Unable to download cloud-sdk-pipeline-config-schema.json. Please ensure that your jenkins is able to download cloud-sdk-pipeline-config-schema.json\n" +
            "You can try this with running an example pipeline which executes: curl --location --remote-name " +
            "\"https://raw.githubusercontent.com/SchemaStore/schemastore/master/src/schemas/json/cloud-sdk-pipeline-config-schema.json\" " +
            "If the error persists, please file a ticket at: https://github.com/SAP/cloud-s4-sdk-pipeline/issues/new/choose")
    }
}

private void generateWarnings(def results) {
    if (!results){
        return
    }
    String htmlSummary = "<h2>Config schema validation warnings</h2>\n"
    String htmlList = htmlListOfSchemaViolations(results)
    String resultsList = listOfSchemaViolations(results)

    addBadge(icon: "warning.gif", text: "Config schema validation warnings:\n${resultsList}")
    htmlSummary += "<h3>Your pipeline configuration is not compliant with the configuration schema </h3> \n" +
        "<p>This is a list of warnings, where your <code>.pipeline/config.yml</code> file violates the configuration schema. Note, it is possible that the results contain false positives.</p>\n" +
        htmlList

    createSummary(icon: "warning.gif", text: htmlSummary)
}

@NonCPS
private String htmlListOfSchemaViolations(def results) {
    String htmlList="";

    for (result in results){
        if (result) {
            htmlList += "<li>Parameter ${result.dataPath} ${result.message} (${result.params})</li>\n"
        }
    }

    if (!htmlList){
        htmlList = "Unable to generate list of JSON schema violations, please <a href=\"https://github.com/SAP/cloud-s4-sdk-pipeline/issues/new/choose\">file a ticket</a>"
        return htmlList
    }

    return "<ol> ${htmlList} </ol>"
}

@NonCPS
private String listOfSchemaViolations(def results) {
    String resultsList="";

    for (result in results){
        if (result) {
            resultsList += "Parameter ${result.dataPath} ${result.message} (${result.params})\n"
        }
    }

    if (!resultsList){
        resultsList = "Unable to generate list of JSON schema violations, please file a ticket: https://github.com/SAP/cloud-s4-sdk-pipeline/issues/new/choose"
    }

    return resultsList
}
