import com.sap.piper.ConfigurationLoader
import com.sap.cloud.sdk.s4hana.pipeline.Analytics

def call(Map parameters) {
    def script = parameters.script

    checkNotUsingWhiteSourceOrgToken(script)

    boolean configurationConverted = false
    configurationConverted = renameMavenStep(script) || configurationConverted
    configurationConverted = removeMavenGlobalSettings(script) || configurationConverted
    configurationConverted = convertCloudfoundryDeployment(script) || configurationConverted
    configurationConverted = convertNeoDeployment(script) || configurationConverted
    configurationConverted = renameBackendIntegrationTests(script) || configurationConverted

    if(configurationConverted) {
        offerMigratedConfigurationAsArtifact(script)
        Analytics.instance.legacyConfig(configurationConverted)
    }
}

def offerMigratedConfigurationAsArtifact(script){
    writeYaml file: 'pipeline_config_new.yml', data: script.commonPipelineEnvironment.configuration
    archiveArtifacts artifacts:'pipeline_config_new.yml'
    echo "[WARNING]: You are using a legacy configuration parameter which might not be supported in the future. "
        "Please change the configuration in your pipeline_config.yml using the content of the file pipeline_config_new.yml " +
        "in the artifacts of this build as inspiration."
}

boolean renameBackendIntegrationTests(script) {
    Map stageConfiguration = script.commonPipelineEnvironment.configuration.stages

    if (stageConfiguration?.integrationTests) {

        if (stageConfiguration.backendIntegrationTests) {
            error("You are using two different configuration keys for the backend integration tests stage. " +
                "Please use the key backendIntegrationTests and remove the key integrationTests from the configuration file. For more information on how to configure the backendIntegrationTests please visit https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/configuration.md#backendintegrationtests")
        }

        stageConfiguration.backendIntegrationTests = stageConfiguration.integrationTests

        echo "[WARNING]: The configuration key integrationTests in the stages configuration should not be used anymore. " +
            "Please use backendIntegrationTests instead. For more information on how to configure the backendIntegrationTests please visit https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/configuration.md#backendintegrationtests"

        stageConfiguration.remove('integrationTests')
        return true
    }
    return false
}

def renameMavenStep(script) {
    Map stepsConfiguration = script.commonPipelineEnvironment.configuration.steps

    if (stepsConfiguration?.executeMaven) {

        if (stepsConfiguration.mavenExecute) {
            error("Your pipeline configuration may only contain the step configuration mavenExecute. " +
                "The key executeMaven should not be used anymore.")
        }

        stepsConfiguration.mavenExecute = stepsConfiguration.executeMaven

        echo "[WARNING]: The configuration key executeMaven in the steps configuration should not be used anymore. " +
            "Please use mavenExecute instead."

        stepsConfiguration.remove('executeMaven')
        return true
    }
    return false
}

def removeMavenGlobalSettings(script) {
    Map mavenConfiguration = ConfigurationLoader.stepConfiguration(script, 'mavenExecute')

    // Maven globalSettings obsolete since introduction of DL-Cache
    if (mavenConfiguration.globalSettingsFile) {
        // switch to project settings if not also defined by user
        String projectSettingsFile = mavenConfiguration.projectSettingsFile
        if (!projectSettingsFile) {
            println("[WARNING]: Your pipeline configuration contains the obsolete configuration parameter " +
                "'executeMaven.globalSettingsFile=${mavenConfiguration.globalSettingsFile}'. The Cloud " +
                "SDK Pipeline uses an own global settings file to inject its download proxy as maven repository mirror. " +
                "Since you did not specify a project settings file, your settings file will be used as " +
                "'executeMaven.projectSettingsFile'.")

            mavenConfiguration.projectSettingsFile = mavenConfiguration.globalSettingsFile
            mavenConfiguration.remove('globalSettingsFile')
        } else {
            currentBuild.result = 'FAILURE'
            error("Your pipeline configuration contains the obsolete configuration parameter " +
                "'executeMaven.globalSettingsFile=${mavenConfiguration.globalSettingsFile}' together with " +
                "'executeMaven.projectSettingsFile=${mavenConfiguration.projectSettingsFile}'. " +
                "The Cloud SDK Pipeline uses an own global settings file to inject its download proxy " +
                "as maven repository mirror. Please reduce your settings to one file and specify " +
                "it under 'executeMaven.globalSettingsFile'.")
        }
        return true
    }
    return false
}

def checkNotUsingWhiteSourceOrgToken(script) {
    Map stageConfig = ConfigurationLoader.stageConfiguration(script, 'whitesourceScan')
    if (stageConfig) {
        if (stageConfig?.orgToken) {
            error("Your pipeline configuration may not use 'orgtoken' in whiteSourceScan stage. "+
                "Store it as a 'Secret Text' in Jenkins and use the 'credentialsId' field.")
        }
        if (!stageConfig?.credentialsId) {
            error("Your pipeline is using 'whiteSourceScan' and has a mandatory parameter 'credentialsId' not configured.")
        }
    }
}

def convertCloudfoundryDeployment(script){
    Map stepsConfiguration = script.commonPipelineEnvironment.configuration.steps

    if(stepsConfiguration?.deployToCfWithCli) {
        Map oldStepConfiguration = stepsConfiguration?.deployToCfWithCli
        stepsConfiguration.cloudFoundryDeploy = [
            dockerImage        : oldStepConfiguration.dockerImage,
            smokeTestStatusCode: oldStepConfiguration.smokeTestStatusCode,
            cloudFoundry       : [
                org          : oldStepConfiguration.org,
                space        : oldStepConfiguration.space,
                appName      : oldStepConfiguration.appName,
                manifest     : oldStepConfiguration.manifest,
                credentialsId: oldStepConfiguration.credentialsId,
                apiEndpoint  : oldStepConfiguration.apiEndpoint
            ]
        ]

        stepsConfiguration.remove('deployToCfWithCli')
        return true
    }
    return false
}

def convertNeoDeployment(script) {
    Map stepsConfiguration = script.commonPipelineEnvironment.configuration.steps
    if(stepsConfiguration?.deployToNeoWithCli) {
        stepsConfiguration.neoDeploy = stepsConfiguration.deployToNeoWithCli
        stepsConfiguration.remove('deployToNeoWithCli')
        return true
    }
    return false
}
