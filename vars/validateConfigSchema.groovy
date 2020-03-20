import groovy.json.JsonBuilder

def call(Map parameters) {
    Script script = parameters.script
    def configFileContent = readYaml file: getConfigLocation(script: script)
    def configFileAsJson = new JsonBuilder(configFileContent).toPrettyString()
    writeFile file: 'pipeline_config.json', text: configFileAsJson
    int status = sh(script: "curl --location --remote-name \"https://raw.githubusercontent.com/SchemaStore/schemastore/master/src/schemas/json/cloud-sdk-pipeline-config-schema.json\"", returnStatus: true)
    if (status == 0 && !configFileContent.general?.extensionRepository) {
        executeNpm(script: script, defaultNpmRegistry: 'https://registry.npmjs.org/') {
            sh(script: "npx ajv-cli validate -s 'cloud-sdk-pipeline-config-schema.json' -d 'pipeline_config.json' --all-errors", returnStatus: true)
        }
    }
}
