import com.sap.piper.ConfigurationLoader
import com.sap.cloud.sdk.s4hana.pipeline.Analytics

def call(Map parameters) {
    Script script = parameters.script

    checkNotUsingWhiteSourceOrgToken(script)

    boolean configurationConverted = false
    configurationConverted = renameMavenStep(script) || configurationConverted
    configurationConverted = removeMavenGlobalSettings(script) || configurationConverted
    configurationConverted = convertCloudfoundryDeployment(script) || configurationConverted
    configurationConverted = convertNeoDeployment(script) || configurationConverted
    configurationConverted = renameBackendIntegrationTests(script) || configurationConverted
    configurationConverted = convertDebugReportConfig(script) || configurationConverted
    checkStaticCodeChecksConfig(script)

    if (configurationConverted) {
        offerMigratedConfigurationAsArtifact(script)
        Analytics.instance.legacyConfig(configurationConverted)
    }
}

def offerMigratedConfigurationAsArtifact(Script script) {
    writeYaml file: 'pipeline_config_new.yml', data: script.commonPipelineEnvironment.configuration
    archiveArtifacts artifacts: 'pipeline_config_new.yml'
    echo "[WARNING]: You are using a legacy configuration parameter which might not be supported in the future. " +
        "Please change the configuration in your .pipeline/config.yml using the content of the file pipeline_config_new.yml " +
        "in the artifacts of this build as inspiration."
}

boolean renameBackendIntegrationTests(Script script) {
    Map stageConfiguration = script.commonPipelineEnvironment.configuration.stages
    if (!stageConfiguration?.integrationTests)
        return false

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

boolean renameMavenStep(Script script) {
    Map stepsConfiguration = script.commonPipelineEnvironment.configuration.steps
    if (!stepsConfiguration?.executeMaven)
        return false

    if (stepsConfiguration.mavenExecute) {
        error("Your pipeline configuration may only contain the step configuration 'mavenExecute'. " +
            "The key 'executeMaven' should not be used anymore.")
    }

    stepsConfiguration.mavenExecute = stepsConfiguration.executeMaven

    echo "[WARNING]: The configuration key executeMaven in the steps configuration should not be used anymore. " +
        "Please use mavenExecute instead."

    stepsConfiguration.remove('executeMaven')
    return true
}

boolean removeMavenGlobalSettings(Script script) {
    Map mavenConfiguration = ConfigurationLoader.stepConfiguration(script, 'mavenExecute')
    // Maven globalSettings obsolete since introduction of DL-Cache
    if (!mavenConfiguration.globalSettingsFile)
        return false

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

def checkNotUsingWhiteSourceOrgToken(Script script) {
    Map stageConfig = ConfigurationLoader.stageConfiguration(script, 'whitesourceScan')
    if (!stageConfig)
        return

    if (stageConfig?.orgToken) {
        error("Your pipeline configuration may not use 'orgtoken' in whiteSourceScan stage. " +
            "Store it as a 'Secret Text' in Jenkins and use the 'credentialsId' field.")
    }
    if (!stageConfig?.credentialsId) {
        error("Your pipeline is using 'whiteSourceScan' and has a mandatory parameter 'credentialsId' not configured.")
    }
}

boolean convertCloudfoundryDeployment(Script script) {
    Map stepsConfiguration = script.commonPipelineEnvironment.configuration.steps
    if (!stepsConfiguration?.deployToCfWithCli)
        return false

    if (stepsConfiguration.cloudFoundryDeploy) {
        error("Your pipeline configuration may only contain the step configuration 'cloudFoundryDeploy'. " +
            "The key 'deployToCfWithCli' should not be used anymore.")
    }

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

boolean convertNeoDeployment(Script script) {
    Map stepsConfiguration = script.commonPipelineEnvironment.configuration.steps
    if (!stepsConfiguration?.deployToNeoWithCli)
        return false

    if (stepsConfiguration.neoDeploy) {
        error("Your pipeline configuration may only contain the step configuration 'neoDeploy'. " +
            "The key 'deployToNeoWithCli' should not be used anymore.")
    }

    stepsConfiguration.neoDeploy = stepsConfiguration.deployToNeoWithCli
    stepsConfiguration.remove('deployToNeoWithCli')
    return true
}

boolean convertDebugReportConfig(Script script) {
    Map postActionConfiguration = script.commonPipelineEnvironment.configuration.postActions
    if (!postActionConfiguration?.archiveDebugLog)
        return false

    if (script.commonPipelineEnvironment.configuration.steps == null) {
        script.commonPipelineEnvironment.configuration.steps = [:]
    }
    Map stepsConfiguration = script.commonPipelineEnvironment.configuration.steps

    if (stepsConfiguration.debugReportArchive) {
        error("Your pipeline configuration may only contain the step configuration 'debugReportArchive'. " +
            "The key 'postActionArchiveDebugLog' should not be used anymore.")
    }

    stepsConfiguration.debugReportArchive = postActionConfiguration.archiveDebugLog
    postActionConfiguration.remove('archiveDebugLog')
    return true
}

void checkStaticCodeChecksConfig(Script script) {
    Map stageConfiguration = script.commonPipelineEnvironment.configuration.stages
    Map stepConfiguration = script.commonPipelineEnvironment.configuration.steps

    if (stageConfiguration?.staticCodeChecks || stepConfiguration?.staticCodeChecks || stepConfiguration?.mavenExecuteStaticCodeCheck?.pmdExcludes) {
        error("The configuration of the static code checks has been moved to the steps configuration." +
            "The configuration options have also changed. Please visit https://sap.github.io/jenkins-library/steps/mavenExecuteStaticCodeChecks/ for more information.")
    }
}
