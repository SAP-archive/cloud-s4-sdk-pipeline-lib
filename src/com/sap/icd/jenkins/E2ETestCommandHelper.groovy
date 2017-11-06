package com.sap.icd.jenkins

class E2ETestCommandHelper implements Serializable{
    static String generate(EndToEndTestType type, String appUrl){
        switch (type){
            case EndToEndTestType.END_TO_END_TEST:
                return "npm run ${EndToEndTestType.END_TO_END_TEST.npmScriptName} -- --headless --launchUrl=${appUrl} || echo 'End to End Tests Failed!'"
            case EndToEndTestType.SMOKE_TEST:
                return "npm run ${EndToEndTestType.SMOKE_TEST.npmScriptName} -- --headless --launchUrl=${appUrl}"
            default:
                throw new RuntimeException("Unknown EndToEndTestType: ${type}")
        }
    }
}
