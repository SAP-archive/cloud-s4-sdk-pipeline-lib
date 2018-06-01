def call(parameters = [:]) {
    handleStepErrors(stepName: 'checkDependencies', stepParameters: parameters) {
        def script = parameters.script

        mavenExecute script: script, flags: "--batch-mode -DoutputFile=mvnDependencyTree.txt", m2Path: s4SdkGlobals.m2Directory, goals: "dependency:tree"

        sh "mkdir -p ${s4SdkGlobals.reportsDirectory}/maven"
        sh "mv unit-tests/mvnDependencyTree.txt ${s4SdkGlobals.reportsDirectory}/maven/unit_test_dependencies.txt"
        sh "mv integration-tests/mvnDependencyTree.txt ${s4SdkGlobals.reportsDirectory}/maven/integration_test_dependencies.txt"

        def unitTestDependencies = readFile("${s4SdkGlobals.reportsDirectory}/maven/unit_test_dependencies.txt")
        def integrationTestDependencies = readFile("${s4SdkGlobals.reportsDirectory}/maven/integration_test_dependencies.txt")

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
