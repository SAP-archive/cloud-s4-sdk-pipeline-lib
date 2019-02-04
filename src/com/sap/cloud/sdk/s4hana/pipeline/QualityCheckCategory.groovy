package com.sap.cloud.sdk.s4hana.pipeline

enum QualityCheckCategory {
    TestAutomation("Test Automation"),
    StaticCodeChecks("Static Code Checks"),
    SecurityScans("Security Scan"),
    PerformanceTests("Performance Tests"),
    S4sdkQualityChecks("SAP S/4HANA Cloud SDK Quality Checks")

    private String label

    QualityCheckCategory(String label) {
        this.label = label
    }

    @Override
    String toString(){
        return label
    }
}
