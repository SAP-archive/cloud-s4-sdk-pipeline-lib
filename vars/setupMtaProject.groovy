import com.cloudbees.groovy.cps.NonCPS
import com.sap.cloud.sdk.s4hana.pipeline.Analytics
import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.piper.DebugReport

def call(Map parameters = [:]) {
    Script script = parameters.script
    Map generalConfiguration = parameters.generalConfiguration
    Map mta = readYaml file: 'mta.yaml'

    List listOfMtaModules = mta.modules

    if (listOfMtaModules == null || listOfMtaModules.isEmpty()) {
        error "No modules found in mta.yaml file, but at least one module is required."
    }

    Map moduleTypeToListOfModules = groupModulesByType(listOfMtaModules)

    assertCorrectMtaProjectStructure(moduleTypeToListOfModules)

    for(entry in moduleTypeToListOfModules.entrySet()) {
        echo entry.getKey() + " has modules:  " + entry.getValue().join(" - ")
    }

    generalConfiguration.projectName = mta.ID

    BuildToolEnvironment.instance.setModulesMap(moduleTypeToListOfModules)

    // TODO Need salt
    Analytics.instance.hashProject(mta.ID)
    DebugReport.instance.modulesMap = BuildToolEnvironment.instance.modulesMap
}

// Example:
// Given: [["type": "java", "path": "srv"] , ["type": "nodejs", "path": "app"]]
// Produces: {java=[srv], nodejs=[app]}
@NonCPS
Map groupModulesByType(List listOfMtaModules) {
    return listOfMtaModules
        .groupBy { module -> module.type }
        .collectEntries { type, module -> [(type): module.path] }
}

def assertCorrectMtaProjectStructure(Map moduleTypeToListOfModules) {
    moduleTypeToListOfModules['java']?.each { javaModule ->
        if (fileExists("${javaModule}/unit-tests")) {
            error "Outdated or unsupported project structure for SAP Cloud Application Programming Model detected.\n" +
                "The module ${javaModule} contains a 'unit-tests' module, which indicates that it uses an outdated and/or not supported project structure.\n" +
                "Please adapt your project to the new structure as described in https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/doc/pipeline/build-tools.md#sap-cloud-application-programming-model--mta.\n" +
                "In case you cannot adapt the project structure, please use a fixed version. The last version of the pipeline supporting this structure is v17." +
                "The version can be configured as described here: https://github.com/SAP/cloud-s4-sdk-pipeline#versioning."
        }

        if (fileExists("${javaModule}/integration-tests")) {
            error "Outdated or unsupported project structure for SAP Cloud Application Programming Model detected.\n" +
                "The module ${javaModule} contains a 'integration-tests' module, which indicates that it uses an outdated and/or not supported project structure.\n" +
                "Please adapt your project to the new structure as described in https://github.com/SAP/cloud-s4-sdk-pipeline/blob/master/doc/pipeline/build-tools.md#sap-cloud-application-programming-model--mta.\n" +
                "In case you cannot adapt the project structure, please use a fixed version. The last version of the pipeline supporting this structure is v20." +
                "The version can be configured as described here: https://github.com/SAP/cloud-s4-sdk-pipeline#versioning."
        }
    }
}
