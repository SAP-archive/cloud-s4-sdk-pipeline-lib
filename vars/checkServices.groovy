import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkServices', stepParameters: parameters) {
        assertPluginIsActive('pipeline-utility-steps')

        Set<String> parameterKeys = ['nonErpDestinations', 'customODataServices', 'nonErpUrls']
        final Map configuration = parameters.subMap(parameterKeys)

        final Set<String> nonErpDestinations = configuration.nonErpDestinations
        final Set<String> nonErpUrls = configuration.nonErpUrls
        final Set<String> customODataServices = configuration.customODataServices

        checkODataServices(nonErpDestinations, customODataServices, nonErpUrls)
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

private void checkODataServices(Set<String> nonErpDestinations, Set<String> customODataServices, Set<String> nonErpUrls) {
    final Set<String> allowedServiceNames = []
    List services = downloadServicesList()
    if (services.empty) {
        echo "List of services is empty, skipping check."
        return
    }

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
        // < SDKv3.2.0 columns: [destination, serviceUrl, entityName, threadName]
        // >= SDKv3.2.0 columns: [serviceUrlHost, servicePath, entityName, threadName]
        String destinationOrServiceUrl = reportAsCsvRecords[i][0].replace('\"', '')
        String usedService = reportAsCsvRecords[i][1].replace('\"', '')
        String entityName = reportAsCsvRecords[i][2].replace('\"', '')

        // SDK does not log the scheme of the URI therefore we do not do any further sanity checks and treat Urls the same way as destinations
        if (!nonErpDestinations?.contains(destinationOrServiceUrl) && !nonErpUrls?.any { nonErpUrl -> destinationOrServiceUrl.startsWith(nonErpUrl)} ) {
            String[] tokenizedService = usedService.tokenize('/')
            // Until SDKv3.9.0 the service url contains the entity name
            if(tokenizedService.last() == entityName){
                usedServiceNames.add(tokenizedService[tokenizedService.length-2])
            }
            else {
                usedServiceNames.add(tokenizedService.last())
            }
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
        def message = "Failed to download the list of available services from API Business Hub (https://api.sap.com/). " +
            "If you encounter this, please open an issue at https://github.com/SAP/cloud-s4-sdk-pipeline/issues/new/choose.\nException: $e"
        def html = 'Failed to download the list of available services from API Business Hub (<a href="https://api.sap.com/">api.sap.com</a>).<br/>' +
            'If you encounter this, please open an issue <a href="https://github.com/SAP/cloud-s4-sdk-pipeline/issues/new/choose">here</a>.<br/>Exception: ' + e
        markBuildAsUnstable(message: message, htmlFormattedMessage: html)
        return []
    }

    if (!serviceJson?.d?.results) {
        markBuildAsUnstable(message: "Response from API Business Hub (https://api.sap.com/) did not fit the expected format.")
        return []
    }

    return serviceJson.d.results
}

