package com.sap.cloud.sdk.s4hana.pipeline

import java.security.MessageDigest

@Singleton
class Analytics implements Serializable {
    static final long serialVersionUID = 1L
    Map telemetryData = [:]
    Map systemInfo = [:]
    Map jobConfiguration = [:]
    String salt = null

    void initAnalytics(boolean isProductive) {
        initTelemetryData()
        initSystemInfo()
        initJobConfiguration(isProductive)
    }

    void initTelemetryData() {
        telemetryData.swaUrl = 'https://webanalytics.cfapps.eu10.hana.ondemand.com/tracker/log'
        telemetryData.action_name = 'SAP S/4HANA Cloud SDK'
        telemetryData.idsite = '70aeb424-0d69-0265-c486-b5471b014ba8'
        telemetryData.url = 'https://github.com/SAP/cloud-s4-sdk-pipeline/tree/master/doc/operations/analytics.md'
    }

    void initSystemInfo() {
        systemInfo.osName = System.getProperty('os.name')
        systemInfo.osVersion = System.getProperty('os.version')
        systemInfo.locale = System.getenv('LANG')
        systemInfo.custom7 = 'jenkins_version'
        systemInfo.e_7 = System.getenv('JENKINS_VERSION')
    }

    void initJobConfiguration(boolean isProductive) {
        jobConfiguration.custom9 = 'is_productive'
        jobConfiguration.e_9 = isProductive
    }

    Map getTelemetryData() {
        return telemetryData
    }

    Map getSystemInfo() {
        return systemInfo
    }

    Map getJobConfiguration() {
        return jobConfiguration
    }

    void hashBuildUrl(def jobUrl) {
        telemetryData.custom1 = 'build_url_hash'
        telemetryData.e_a = hash(jobUrl) // This is how SWA fields are defined: e_a, e_2, e_3 etc.
    }

    void hashProject(String projectId, String salt) {
        telemetryData.custom2 = 'project_id_hash'
        telemetryData.e_2 = hash(projectId, salt ?: this.salt)
    }

    void legacyConfig(boolean legacy) {
        jobConfiguration.custom8 = 'legacy'
        jobConfiguration.e_8 = legacy
    }

    private String hash(String input) {
        return hash(input, salt)
    }

    private String hash(String input, String salt) {
        if (salt == null || salt.isEmpty()) {
            // Don't hash without salt to don't compromise on privacy
            return 'NOT-HASHABLE'
        }
        MessageDigest messageDigest = MessageDigest.getInstance('SHA-1')
        messageDigest.reset()
        messageDigest.update(salt.bytes)
        return messageDigest.digest(input.bytes).encodeHex().toString()
    }
}
