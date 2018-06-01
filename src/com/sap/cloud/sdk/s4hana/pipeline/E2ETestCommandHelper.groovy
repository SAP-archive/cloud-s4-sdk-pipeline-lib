package com.sap.cloud.sdk.s4hana.pipeline

class E2ETestCommandHelper implements Serializable {
    static String generate(EndToEndTestType type, String appUrl) {
        switch (type) {
            case EndToEndTestType.END_TO_END_TEST:
                return "npm run ${EndToEndTestType.END_TO_END_TEST.npmScriptName} -- --launchUrl=${appUrl}"
            case EndToEndTestType.SMOKE_TEST:
                return "npm run ${EndToEndTestType.SMOKE_TEST.npmScriptName} -- --launchUrl=${appUrl}"
            default:
                throw new RuntimeException("Unknown EndToEndTestType: ${type}")
        }
    }
}
