import groovy.json.JsonBuilder
import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

def call(Map parameters) {
    handleStepErrors(stepName: 'initS4SdkPipelineLibrary', stepParameters: parameters) {
        def script = parameters.script
        if (!parameters.configFile) {
            parameters.configFile = getConfigLocation(script: script)
        }
        parameters.customDefaults = parameters.customDefaults ?: ['default_s4_pipeline_environment.yml']
        setupCommonPipelineEnvironment(parameters)
        convertLegacyConfiguration script: script
        if (!Boolean.valueOf(env.ON_K8S)) {
            checkDiskSpace script: script
        }
        setupDownloadCache script: script

        if (!env.BRANCH_NAME) {

            String message = "Please use Multibranch Pipeline Job. \n" +
                "The pipeline is designed for the Multibranch Pipeline job type. It appears that it is running in a Singlebranch Pipeline. \n" +
                "Please refer to 'https://jenkins.io/doc/book/pipeline/multibranch/#creating-a-multibranch-pipeline' for information on how to setup a Multibranch Pipeline. \n"

            assertPluginIsActive('badge')
            addBadge(icon: "warning.gif", text: message)
            createSummary(icon: "warning.gif", text: "<h2>Please use multi branch pipeline</h2>\n" + message)
        }

        def configFileContent = readYaml file: parameters.configFile
        def configFileAsJson = new JsonBuilder(configFileContent).toPrettyString()
        writeFile file: 'pipeline_config.json', text: configFileAsJson
        int status = sh(script: "curl --location --remote-name \"https://raw.githubusercontent.com/SchemaStore/schemastore/master/src/schemas/json/cloud-sdk-pipeline-config-schema.json\"", returnStatus: true)
        if (status == 0 && !configFileContent.general?.extensionRepository) {
            executeNpm(script: script, defaultNpmRegistry: 'https://registry.npmjs.org/') {
                sh(script: "npx ajv-cli validate -s 'cloud-sdk-pipeline-config-schema.json' -d 'pipeline_config.json' --all-errors", returnStatus: true)
            }
        }
    }
}
