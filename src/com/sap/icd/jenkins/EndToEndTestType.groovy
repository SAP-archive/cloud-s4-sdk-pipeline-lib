package com.sap.icd.jenkins

enum EndToEndTestType {
    END_TO_END_TEST('ci-e2e'),
    SMOKE_TEST('ci-smoke')

    final String npmScriptName

    EndToEndTestType(String npmScriptName){
        this.npmScriptName = npmScriptName
    }
}
