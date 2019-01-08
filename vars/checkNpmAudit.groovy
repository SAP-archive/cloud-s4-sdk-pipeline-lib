import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkNpmAudit', stepParameters: parameters) {
        assertPluginIsActive('badge')

        final script = parameters.script
        final Map configuration = parameters.configuration
        final String basePath = parameters.basePath

        executeNpmAudit(script, configuration, basePath)
    }
}

private void executeNpmAudit(def script, Map configuration, String basePath) {
    dir(basePath) {
        executeNpm(script: script) {
            sh "echo 'Falling back to default public npm registry while executing npm audit check.' && npm config delete registry"
            sh script: "npm audit --json > npm-audit.json", returnStatus: true
        }

        def npmAuditResult = readJSON file: "npm-audit.json"


        Map advisories = filterUserAuditedAdvisories(configuration, npmAuditResult.advisories)

        def (Map critical, Map high, Map moderate) = splitBySeverity(advisories)

        Map vulnerabilitySummary = [
            critical: critical.size(),
            high    : high.size(),
            moderate: moderate.size()
        ]

        if (vulnerabilitySummary.critical > 0 || vulnerabilitySummary.high > 1 || vulnerabilitySummary.moderate > 5) {
            script.currentBuild.setResult('FAILURE')
            def npmAuditSummary = "npm dependency audit discovered ${vulnerabilitySummary.critical} crticial and ${vulnerabilitySummary.high} high vulnerabilities. " +
                "Please execute 'npm audit' locally to identify and fix relevant findings.\n" +
                "Summary of the findings:\n" +
                "${formatRelevantAdvisoriesForLog(critical, high, moderate)}"
            addBadge(icon: "error.gif", text: npmAuditSummary)
            createSummary(icon: "error.gif", text: "<h2>npm dependency audit discovered ${vulnerabilitySummary.critical} crticial and ${vulnerabilitySummary.high} high vulnerabilities</h2>\n" +
                "Please execute <code>npm audit</code> locally to identify and fix relevant findings.\n" +
                "<h3>Summary of the findings</h3>\n" + formatRelevantAdvisoriesForBadge(critical, high, moderate))
            error npmAuditSummary
        }
    }
}

private List splitBySeverity(Map advisories) {
    Map critical = advisories.findAll { it.value.severity == 'critical' }
    Map high = advisories.findAll { it.value.severity == 'high' }
    Map moderate = advisories.findAll { it.value.severity == 'moderate' }
    return [critical, high, moderate]
}

private Map filterUserAuditedAdvisories(Map configuration, Map advisories) {
    List userAuditedAdvisories = configuration?.auditedAdvisories

    if (userAuditedAdvisories) {
        List unmatchedUserAuditedAdvisories = userAuditedAdvisories.minus(advisories.keySet())
        List matchedUserAuditedAdvisories = userAuditedAdvisories.minus(unmatchedUserAuditedAdvisories)

        String htmlSummary = "<h2>npm dependency audit warnings</h2>\n"

        if (!matchedUserAuditedAdvisories.empty) {
            addBadge(icon: "warning.gif", text: "Ignoring audited npm advisories:\n ${matchedUserAuditedAdvisories.join("\n")}")
            htmlSummary += "<h3>Ignoring audited npm advisories</h3> \n" +
                "<p>This is a list of advisories which are marked as <em>audited</em> in your <code>pipeline_config.yml</code> file. \n" +
                htmlListOfUserAuditedAdvisories(matchedUserAuditedAdvisories)
        }

        if (!unmatchedUserAuditedAdvisories.empty) {
            addBadge(icon: "warning.gif", text: "Discovered audited npm advisories which don't apply to this project:\n ${unmatchedUserAuditedAdvisories.join("\n")}")
            htmlSummary += "<h3>Discovered audited npm advisories which don't apply to this project</h3> \n" +
                "<p>Please review the following advisories in your <code>pipeline_config.yml</code> file and consider removing them.</p>\n" +
                htmlListOfUserAuditedAdvisories(unmatchedUserAuditedAdvisories)
        }

        if (htmlSummary != "<h2>npm dependency audit warnings</h2>\n") {
            createSummary(icon: "warning.gif", text: htmlSummary)
        }

        advisories = advisories.findAll { !(it.key in userAuditedAdvisories) }
    }

    return advisories
}

private String htmlListOfUserAuditedAdvisories(List userAuditedAdvisories) {
    return "<ol>${userAuditedAdvisories.collect { "<li><a target=\"_blank\" href=\"https://npmjs.com/advisories/${it}\">${it}</a></li>" }.join("\n")}</ol>"
}

private String formatRelevantAdvisoriesForBadge(Map critical, Map high, Map moderate) {
    def criticalList = critical.collect { advisoryId, advisoryBody -> formatHtml(advisoryBody) }
    def highList = high.collect { advisoryId, advisoryBody -> formatHtml(advisoryBody) }
    def moderateList = moderate.collect { advisoryId, advisoryBody -> formatHtml(advisoryBody) }

    return criticalList?.collect { "<li>${it}</li>" }?.join('\n') +
        highList?.collect { "<li>${it}</li>" }?.join('\n') +
        moderateList?.collect { "<li>${it}</li>" }?.join('\n')
}

private String formatHtml(advisoryBody) {
    return "${severity(advisoryBody)} <em>${advisoryBody.title}</em> vulnerability found in dependency \"${advisoryBody.module_name}\", " +
        "see <a target=\"_blank\" href=\"${advisoryBody.url}\">${advisoryBody.url}</a> for details."
}

private String formatRelevantAdvisoriesForLog(Map critical, Map high, Map moderate) {
    def criticalList = critical.collect { advisoryId, advisoryBody -> format(advisoryBody) }
    def highList = high.collect { advisoryId, advisoryBody -> format(advisoryBody) }
    def moderateList = moderate.collect { advisoryId, advisoryBody -> format(advisoryBody) }

    return criticalList?.join('\n') + highList?.join('\n') + moderateList?.join('\n')
}

private String format(advisoryBody) {
    return "${severity(advisoryBody)} \"${advisoryBody.title}\" vulnerability found in dependency \"${advisoryBody.module_name}\", see ${advisoryBody.url} for details."
}

private String severity(advisoryBody) {
    return (advisoryBody.severity as String).capitalize()
}
