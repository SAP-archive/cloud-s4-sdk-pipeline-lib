import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkServices', stepParameters: parameters) {
        assertPluginIsActive('pipeline-utility-steps')

        Set<String> parameterKeys = ['nonErpDestinations', 'customODataServices']
        final Map configuration = parameters.subMap(parameterKeys)

        final Set<String> nonErpDestinations = configuration.nonErpDestinations
        final Set<String> customODataServices = configuration.customODataServices

        checkODataServices(nonErpDestinations, customODataServices)
        checkBapiServices(nonErpDestinations)
    }
}

private void checkBapiServices(Set<String> nonErpDestinations) {
    // BAPIs and RFMs of SAP_COM_0180
    final Set<String> allowedServiceNames = [
        'BAPI_TRANSACTION_COMMIT',
        'BAPI_TRANSACTION_ROLLBACK',
        'BAPI_COSTCENTER_CREATEMULTIPLE',
        'BAPI_ACCSTMT_CREATEFROMPREVDAY',
        'BAPI_ACC_PRIMARY_COSTS_POST',
        'BAPI_ACC_PRIMARY_COSTS_CHECK',
        'BAPI_ACC_MANUAL_ALLOC_POST',
        'BAPI_ACC_MANUAL_ALLOC_CHECK',
        'BAPI_ACC_DOCUMENT_POST',
        'BAPI_ACC_DOCUMENT_CHECK',
        'BAPI_ACC_ACTIVITY_ALLOC_POST',
        'BAPI_ACC_ACTIVITY_ALLOC_CHECK',
        'BAPI_FIXEDASSET_CHANGE',
        'BAPI_FIXEDASSET_CREATE1',
        'BAPI_FIXEDASSET_GETLIST',
        'BAPI_FTR_FXT_DEALGET',
        'BAPI_FTR_FXOPTION_GETDETAIL',
        'BAPI_FTR_FXT_DEALCREATE',
        'BAPI_FTR_FXT_CREATESWAP',
        'BAPI_FTR_CREATE_FXOPTIONS',
        'FCXL_GET_ASPECT_DEFINITION',
        'FCXL_GET_ATTRIBUTE_VALUE',
        'FCXL_GET_CHAR_ATTRIBUTES',
        'FCXL_GET_CHAR_HIERARCHIES',
        'FCXL_GET_CHAR_SETS',
        'FCXL_GET_CHAR_VALUES',
        'FCXL_GET_CONSOLIDATION_ASPECTS',
        'FCXL_GET_DATA',
        'FCXL_GET_HIERARCHY_VALUES',
        'FCXL_GET_MASS_DATA',
        'FCXL_GET_PROGRAM_TEXTS',
        'FCXL_GET_SET_VALUES',
        'FCXL_GET_UPDATEABLE',
        'FCXL_SET_DATA',
        'FC_ITEM_PROP_GET_RFC',
        'FC_ITGRP_PROP_GET_RFC',
        'FC_GLOBAL_PARAMS_IMPORT_RFC',
        'FC_GLOBAL_PARAMS_EXPORT_RFC'
    ]

    List reportAsCsvRecords = readCSV file: "${s4SdkGlobals.reportsDirectory}/service_audits/aggregated_rfc_audit.log"

    final Set<String> usedServiceNames = []
    for (int i = 0; i < reportAsCsvRecords.size(); i++) {
        // columns: [type, destination, serviceName, threadName]
        String usedDestination = reportAsCsvRecords[i][1].replace('\"', '')
        String usedService = reportAsCsvRecords[i][2].replace('\"', '')

        if (!nonErpDestinations?.contains(usedDestination)) {
            usedServiceNames.add(usedService)
        }
    }

    println "Used RFC Services: " + usedServiceNames

    final Set<String> unallowedUsedServices = usedServiceNames.collect()
    unallowedUsedServices.removeAll(allowedServiceNames)

    if (!unallowedUsedServices.isEmpty()) {
        currentBuild.result = 'FAILURE'
        error("Your project uses non-official RFC services: ${unallowedUsedServices}")
    }
}

private void checkODataServices(Set<String> nonErpDestinations, Set<String> customODataServices) {
    final Set<String> allowedServiceNames = []
    List services = downloadServicesList()
    for (int x = 0; x < services.size(); x++) {
        String serviceName = services[x].Name
        allowedServiceNames.add(serviceName)
    }

    if (customODataServices) {
        allowedServiceNames.addAll(customODataServices)
    }

    List reportAsCsvRecords = readCSV file: "${s4SdkGlobals.reportsDirectory}/service_audits/aggregated_odata_audit.log"

    final Set<String> usedServiceNames = []
    for (int i = 0; i < reportAsCsvRecords.size(); i++) {
        // columns: [destination, serviceUrl, entityName, threadName]
        String usedDestination = reportAsCsvRecords[i][0].replace('\"', '')
        String usedService = reportAsCsvRecords[i][1].replace('\"', '')

        if (!nonErpDestinations?.contains(usedDestination)) {
            usedServiceNames.add(usedService.tokenize('/').last())
        }
    }

    println "Used OData Services: " + usedServiceNames

    final Set<String> unallowedUsedServices = usedServiceNames.collect()
    unallowedUsedServices.removeAll(allowedServiceNames)

    if (!unallowedUsedServices.isEmpty()) {
        currentBuild.result = 'FAILURE'
        error("Your project uses non-official OData services: ${unallowedUsedServices}")
    }
}

private List downloadServicesList() {
    String serviceCatalogUrl = 'https://api.sap.com/odata/1.0/catalog.svc/ContentEntities.ContentPackages(\'SAPS4HANACloud\')/Artifacts?$format=json&$select=Name'

    Map serviceJson = [:]

    try {
        serviceJson = readJSON(text: fetchUrl(serviceCatalogUrl))
    } catch (Exception e) {
        error("Failed to download the list of available services from API Business Hub (https://api.sap.com/). " +
            "Please check if your Jenkins can reach this web resource.\nException: $e")
    }

    if (!serviceJson?.d?.results) {
        error("Response from API Business Hub (https://api.sap.com/) did not fit the expected format.")
    }

    return serviceJson.d.results
}
