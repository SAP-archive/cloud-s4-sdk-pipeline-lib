def call(parameters = [:]) {
    handleStepErrors(stepName: 'checkDependencies', stepParameters: parameters) {
        def script = parameters.script
        String basePath = parameters.basePath

        mavenExecute script: script,
            flags: "--batch-mode -DoutputFile=mvnDependencyTree.txt",
            m2Path: s4SdkGlobals.m2Directory,
            pomPath: "${basePath}/pom.xml",
            goals: "dependency:tree"

        if (fileExists("${basePath}/unit-tests")) {
            def unitTestDependencies = readFile("${basePath}/unit-tests/mvnDependencyTree.txt")

            // check dependencies in unit tests
            if (!(unitTestDependencies =~ /com\.sap\.cloud\.s4hana\.quality:odata-querylistener:jar:.+:test/)) {
                currentBuild.result = 'FAILURE'
                error('odata-querylistener not found in unit tests')
            }
            if (!(unitTestDependencies =~ /com\.sap\.cloud\.s4hana\.quality:rfc-querylistener:jar:.+:test/)) {
                currentBuild.result = 'FAILURE'
                error('rfc-querylistener not found in unit tests')
            }
            if (!(unitTestDependencies =~ /com\.sap\.cloud\.s4hana\.quality:httpclient-listener:jar:.+:test/)) {
                currentBuild.result = 'FAILURE'
                error('httpclient-listener not found in unit tests')
            }
        }

        if (fileExists("${basePath}/integration-tests")) {

            def integrationTestDependencies = readFile("${basePath}/integration-tests/mvnDependencyTree.txt")

            // check dependencies in integration tests
            if (!(integrationTestDependencies =~ /com\.sap\.cloud\.s4hana\.quality:odata-querylistener:jar:.+:test/)) {
                currentBuild.result = 'FAILURE'
                error('odata-querylistener not found in integration tests')
            }
            if (!(integrationTestDependencies =~ /com\.sap\.cloud\.s4hana\.quality:rfc-querylistener:jar:.+:test/)) {
                currentBuild.result = 'FAILURE'
                error('rfc-querylistener not found in integration tests')
            }
            if (!(integrationTestDependencies =~ /com\.sap\.cloud\.s4hana\.quality:httpclient-listener:jar:.+:test/)) {
                currentBuild.result = 'FAILURE'
                error('httpclient-listener not found in integration tests')
            }

        }
    }
}
