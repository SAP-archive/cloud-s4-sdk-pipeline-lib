import com.sap.piper.ConfigurationMerger

@Grab('com.xlson.groovycsv:groovycsv:1.1')
import static com.xlson.groovycsv.CsvParser.parseCsv

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkServices', stepParameters: parameters) {
        Set parameterKeys = ['nonErpDestinations']

        final Map configuration = parameters.subMap(parameterKeys)

        final Set<String> nonErpDestinations = configuration.nonErpDestinations

        checkODataServices(nonErpDestinations)
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

    //println "Allowed BAPI / RFC Services: " + allowedServiceNames

    String reportFileAsString = readFile("${s4SdkGlobals.reportsDirectory}/service_audits/aggregated_rfc_audit.log")
    List<String> columns = [
        'type',
        'destination',
        'serviceName',
        'threadName'
    ]
    def reportAsCsv = parseCsv([readFirstLine: true, columnNames: columns], reportFileAsString)

    final Set<String> usedServiceNames = []
    for (line in reportAsCsv) {
        if (!nonErpDestinations?.contains(line.destination)) {
            usedServiceNames.add(line.serviceName)
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

private void checkODataServices(Set<String> nonErpDestinations) {
    final Set<String> allowedServiceNames = []
    def serviceJson = readJSON(text: fetchUrl('https://api.sap.com/odata/1.0/catalog.svc/ContentEntities.ContentPackages(\'SAPS4HANACloud\')/Artifacts?$format=json&$select=Name'))
    for (int x = 0; x < serviceJson.d.results.size(); x++) {
        String serviceName = serviceJson.d.results[x].Name
        allowedServiceNames.add(serviceName)
    }
    //println "Allowed OData Services: " + allowedServiceNames

    String reportFileAsString = readFile("${s4SdkGlobals.reportsDirectory}/service_audits/aggregated_odata_audit.log")
    List<String> columns = [
        'destination',
        'serviceUrl',
        'entityName',
        'threadName'
    ]
    def reportAsCsv = parseCsv([readFirstLine: true, columnNames: columns], reportFileAsString)

    final Set<String> usedServiceNames = []
    for (line in reportAsCsv) {
        if (!nonErpDestinations?.contains(line.destination)) {
            usedServiceNames.add(line.serviceUrl.tokenize('/').last())
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
