def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'aggregateListenerLogs', stepParameters: parameters) {
        dir("${s4SdkGlobals.reportsDirectory}/service_audits") {
            sh "cat odata?audit_*.log > aggregated_odata_audit.log || echo 'Failed to concatenate odata audit files'"
            sh "cat rfc_audit_*.log > aggregated_rfc_audit.log || echo 'Failed to concatenate rfc audit files'"
            sh "cat http_audit_*.log > aggregated_http_audit.log || echo 'Failed to concatenate http audit files'"
        }
    }
}
