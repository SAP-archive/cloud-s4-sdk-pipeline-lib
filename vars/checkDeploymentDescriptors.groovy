import com.cloudbees.groovy.cps.NonCPS
import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkDeploymentDescriptors', stepParameters: parameters) {

        def script = parameters.script

        Set keys = ['unsafeMode']
        Map defaultGeneralConfiguration = ConfigurationLoader.defaultGeneralConfiguration(script)
        Map projectGeneralConfiguration = ConfigurationLoader.generalConfiguration(script)
        Map stageConfiguration = loadEffectiveStageConfiguration(script: script, stageName: "productionDeployment")
        Map stepConfiguration = loadEffectiveStepConfiguration(script: script, stepName: "cloudFoundryDeploy")
        Map generalConfig = ConfigurationMerger.merge(projectGeneralConfiguration, keys, defaultGeneralConfiguration)
        boolean unsafeMode = generalConfig.unsafeMode
        boolean safeMode = !unsafeMode

        Set usedForbiddenEnvironmentVariables = []

        if (isProductiveBranch(script: script)) {

            if (BuildToolEnvironment.instance.isMta()) {
                def mta = readYaml file: 'mta.yaml'
                usedForbiddenEnvironmentVariables.addAll(extractForbiddenEnvironmentVariablesMta(mta))
            }

            List neoTargets = stageConfiguration.neoTargets
            if (neoTargets) {
                usedForbiddenEnvironmentVariables.addAll(extractForbiddenEnvironmentVariablesNeo(neoTargets))
            }

            List cfTargets = stageConfiguration.cfTargets
            if (cfTargets) {
                Set stageLevelManifestFiles = computeStageLevelManifestFiles(cfTargets, stepConfiguration?.cloudFoundry?.manifest, projectGeneralConfiguration?.cloudFoundry?.manifest)

                for (int i = 0; i < stageLevelManifestFiles.size(); i++) {
                    def manifest = readYaml file: stageLevelManifestFiles[i]
                    usedForbiddenEnvironmentVariables.addAll(extractForbiddenEnvironmentVariablesCloudFoundry(manifest))
                }
            }

            if (usedForbiddenEnvironmentVariables) {
                if (safeMode) {
                    error(formatLogMessage(usedForbiddenEnvironmentVariables))
                } else {
                    logUnsafeUsage(usedForbiddenEnvironmentVariables)
                }
            }
        }
    }
}

@NonCPS
private Set extractForbiddenEnvironmentVariablesMta(mtaDescriptor) {
    Set extractedEnvironmentVariables = []
    def modules = mtaDescriptor.modules
    for (int i = 0; i < modules.size(); i++) {
        if (modules[i].properties) {
            extractedEnvironmentVariables.addAll(modules[i].properties.keySet())
        }
    }
    return extractForbiddenEnvironmentVariables(extractedEnvironmentVariables)
}

@NonCPS
private Set extractForbiddenEnvironmentVariablesNeo(List neoTargets) {
    Set extractedEnvironmentVariables = []
    for (int i = 0; i < neoTargets.size(); i++) {
        def environment = neoTargets[i].environment
        if (environment) {
            extractedEnvironmentVariables.addAll(environment.keySet())
        }
    }
    return extractForbiddenEnvironmentVariables(extractedEnvironmentVariables)
}

@NonCPS
private Set computeStageLevelManifestFiles(List cfTargets, String stepManifest, String generalManifest) {
    Set stageLevelManifestFiles = []
    for (int i = 0; i < cfTargets.size(); i++) {
        if (cfTargets[i].manifest) {
            stageLevelManifestFiles.add(cfTargets[i].manifest)
        } else if (stepManifest) {
            stageLevelManifestFiles.add(stepManifest)
        } else if (generalManifest) {
            stageLevelManifestFiles.add(generalManifest)
        }
    }
    return stageLevelManifestFiles
}

@NonCPS
private Set extractForbiddenEnvironmentVariablesCloudFoundry(manifest) {
    Set extractedEnvironmentVariables = []
    def applications = manifest.applications
    for (int i = 0; i < applications.size(); i++) {
        def env = applications[i].env
        if (env) {
            extractedEnvironmentVariables.addAll(env.keySet())
        }
    }
    return extractForbiddenEnvironmentVariables(extractedEnvironmentVariables)
}

@NonCPS
private Set extractForbiddenEnvironmentVariables(Set extractedEnvironmentVariables) {
    final Set forbiddenEnvironmentVariables = ['ALLOW_MOCKED_AUTH_HEADER', 'USE_MOCKED_TENANT', 'USE_MOCKED_USER']
    return extractedEnvironmentVariables.intersect(forbiddenEnvironmentVariables)
}

private logUnsafeUsage(usedForbiddenEnvironmentVariables) {
    if (usedForbiddenEnvironmentVariables) {
        String message = formatLogMessage(usedForbiddenEnvironmentVariables)

        String html =
            """
<h2>Unsafe variables are used in a productive branch</h2>
<p>Following environment variables should never be used in a productive environment:</p>
<p>$usedForbiddenEnvironmentVariables</p>
<p>This is a very high security threat, please consider removing those variables. For more information on securing your application visit:</p>
<p><a href="https://blogs.sap.com/2017/07/18/step-8-with-sap-s4hana-cloud-sdk-secure-your-application-on-sap-cloud-platform-neo/">Secure App on Neo</a></p>
<p><a href="https://blogs.sap.com/2017/07/18/step-7-with-sap-s4hana-cloud-sdk-secure-your-application-on-sap-cloud-platform-cloudfoundry/">Secure App on CloudFoundry</a></p>
"""

        echo message
        assertPluginIsActive('badge')
        addBadge(icon: "warning.gif", text: "Unsafe environment variables are used. Please have a look into the summary or log")
        createSummary(icon: "warning.gif", text: html)
    }
}

private String formatLogMessage(Set usedForbiddenEnvironmentVariables) {
    """The environment variables \n\n\t\"$usedForbiddenEnvironmentVariables\" \n\nset in the deployment descriptor should never be used in a productive environment. \n
This is a very high security threat, please consider removing those variables. For more information on securing your application visit: \n
https://blogs.sap.com/2017/07/18/step-7-with-sap-s4hana-cloud-sdk-secure-your-application-on-sap-cloud-platform-cloudfoundry/ \n
https://blogs.sap.com/2017/07/18/step-8-with-sap-s4hana-cloud-sdk-secure-your-application-on-sap-cloud-platform-neo/ \n"""
}
