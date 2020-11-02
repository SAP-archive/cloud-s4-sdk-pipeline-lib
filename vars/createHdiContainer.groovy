import com.sap.cloud.sdk.s4hana.pipeline.BashUtils
import com.sap.cloud.sdk.s4hana.pipeline.ProjectUtils
import com.sap.piper.ConfigurationHelper
import groovy.transform.Field
import net.sf.json.JSONObject

@Field String STEP_NAME = 'createHdiContainer'

@Field Set GENERAL_CONFIG_KEYS = [
    'cloudFoundry',
    'projectName'
]

@Field Set STEP_CONFIG_KEYS = GENERAL_CONFIG_KEYS.plus([
    'dockerImage',
    'broker'
])

@Field Set PARAMETER_KEYS = STEP_CONFIG_KEYS

@Field String ENV_FILE = "./db/default-env.json"

@Field String DB_CONFIG_FILE = "db/src/.hdiconfig"

@Field String CONNECTION_PROPERTY = "integration-tests/src/test/resources/connection.properties"

def call(Map parameters = [:], Closure body) {

    handleStepErrors(stepName: STEP_NAME, stepParameters: parameters) {
        Script script = parameters.script

        Map configuration = ConfigurationHelper.newInstance(this)
            .loadStepDefaults()
            .mixinGeneralConfig(script.commonPipelineEnvironment, GENERAL_CONFIG_KEYS)
            .mixinStepConfig(script.commonPipelineEnvironment, STEP_CONFIG_KEYS)
            .mixin(parameters, PARAMETER_KEYS)
            .use()

        if (fileExists('integration-tests/pom.xml')) {
            assertPreconditions(configuration)
            String projectName = ProjectUtils.getProjectName(script)
            String hdiContainerName = generateHdiContainerName(projectName)

            echo "Creating a new HDI container ${hdiContainerName}"
            try {
                createContainer(script, configuration, hdiContainerName)
                initializeDatabase(script, hdiContainerName)
                body()
            } finally {
                deleteContainer(script, configuration, hdiContainerName)
            }
        } else {
            body()
        }
    }
}

private assertPreconditions(Map configuration) {
    if (!fileExists(DB_CONFIG_FILE)) {
        error("File ${DB_CONFIG_FILE} not present. Unable to create a HDI container.")
    }

    Map cloudFoundry = configuration.cloudFoundry
    if (!cloudFoundry || !cloudFoundry.credentialsId || !cloudFoundry.space || !cloudFoundry.org || !cloudFoundry.apiEndpoint) {
        error("No credentials or CloudFoundry target configured. But, these are mandatory parameters to create a new HDI " +
            "container.\n Please refer to https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/configuration.md for more " +
            "information on the configuration options.")
    }
}

private insideCfDockerContainer(Script script, Map configuration, Closure body) {
    dockerExecute(script: script, dockerImage: configuration.dockerImage) {
        body()
    }
}

private login(Map cloudFoundry) {
    withCredentials([usernamePassword(credentialsId: cloudFoundry.credentialsId,
        passwordVariable: 'password',
        usernameVariable: 'username')]) {
        sh "cf login -u ${BashUtils.escape(username)} -p ${BashUtils.escape(password)} -a ${BashUtils.escape(cloudFoundry.apiEndpoint)} -o ${BashUtils.escape(cloudFoundry.org)} -s ${BashUtils.escape(cloudFoundry.space)}"
    }
}

private createProperties(Map hdiCredentials) {
    String schema = hdiCredentials.schema
    String user = hdiCredentials.user
    String password = hdiCredentials.password
    /**
     * Convert special characters to ISO-8859-1 encoded character
     * Example: replace \u0026 -> &
     *
     * The encoding is required only while creating the properties file.
     * In the case of the environment file, we use the credentials as-is.
     */
    String url = new String(hdiCredentials.url.getBytes(), "ISO-8859-1")
    String connectionProperties = "schema=${schema}\nusername=${user}\npassword=${password}\nconnectionURL=${url}\n"
    writeFile file: CONNECTION_PROPERTY, text: connectionProperties
}


private createEnv(Map hdiCredentials) {
    Map defaultEnv = [VCAP_SERVICES: [hana: [["name": "hana", "tags": ["hana"], "credentials": hdiCredentials]]]]
    writeJSON file: ENV_FILE, json: JSONObject.fromObject(defaultEnv)
}

private Map getHdiCredentials(String hdiContainer) {
    def serviceKey = sh(returnStdout: true, script: "cf service-key ${hdiContainer} hdi-key")
    def hdiCredentials = serviceKey.split('\n').drop(2).join('\n')
    if (!hdiCredentials) {
        error("Getting credentials for the HDI container failed. Please ensure that the HDI container ${hdiContainer} created and contains a service key with a name `hdi-key`.")
    }

    Map credentials = readJSON text: hdiCredentials

    if (!credentials.schema || !credentials.user || !credentials.password || !credentials.url) {
        error("Missing one or more credentials values. Please ensure that the service-key `hdi-key` contains values for `schema`, `user`, `password` and `url`.")
    }

    return credentials
}

private createContainer(Script script, Map configuration, String hdiContainer) {
    insideCfDockerContainer(script, configuration) {
        login(configuration.cloudFoundry)

        def containerStatus = sh(returnStatus: true, script: "cf service ${hdiContainer}  > /dev/null 2>&1")
        if (containerStatus == 0) {
            error("The HDI Container ${hdiContainer} already exists.")
        }

        String command = "cf create-service hana hdi-shared ${hdiContainer}"
        if (configuration.broker) {
            command += " -b ${BashUtils.escape(configuration.broker)}"
        }

        sh command
        isKeyCreated = 1
        try {
            timeout(time: 180, unit: 'SECONDS') {
                waitUntil {
                    echo "Waiting for DB service and service-key to be created"
                    isKeyCreated = sh(returnStatus: true, script: "cf create-service-key ${hdiContainer} hdi-key > /dev/null 2>&1")
                    return isKeyCreated == 0 ? true : false
                }
            }
        } catch (Exception e) {
            error("Failed to create a service-key. Please ensure that the user has authorization to create a HDI container and to add a service-key to the HDI container.\n" + e.getStackTrace())
        }

        echo "HDI container ${hdiContainer} created"

        Map hdiCredentials = getHdiCredentials(hdiContainer)
        createProperties(hdiCredentials)
        createEnv(hdiCredentials)
    }
}

private initializeDatabase(Script script, String hdiContainer) {
    dir('db') {
        echo "Deploying content"
        npmExecuteScripts script: script, runScripts: ['start'], scriptOptions: ['--exit'], virtualFrameBuffer: false
    }
}

private deleteContainer(Script script, Map configuration, String hdiContainerName) {
    insideCfDockerContainer(script, configuration) {
        login(configuration.cloudFoundry)
        sh "cf delete-service-key ${hdiContainerName} hdi-key -f"
        sh "cf delete-service ${hdiContainerName} -f"
    }
}

private generateHdiContainerName(String projectName) {
    //Shortening the projectName to a maximum of 13 characters as the HDI container name may only have a length of 50
    //and the UUID contains 36 characters plus separator.
    String shortName = projectName.substring(0, Math.min(13, projectName.length()))
    return "${shortName}-${UUID.randomUUID().toString()}"
}
