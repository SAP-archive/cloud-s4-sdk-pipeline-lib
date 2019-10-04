import com.cloudbees.groovy.cps.NonCPS
import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils

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
        if (!(fileExists('package-lock.json') || fileExists('npm-shrinkwrap.json'))) {
            echo "Expected npm package lock file to exist. This is a requirement for npm audit. See https://docs.npmjs.com/files/package-locks for background. Executing `npm install` to create a package-lock.json at '$basePath'"

            def dockerOptions = ['--cap-add=SYS_ADMIN']
            DownloadCacheUtils.appendDownloadCacheNetworkOption(script, dockerOptions)

            executeNpm(script: script, dockerOptions: dockerOptions) {
                sh 'npm install'
            }
        }
        Map discoveredAdvisories
        executeNpm(script: script) {
            echo "Running npm audit check in '${basePath}'. Falling back to default public npm registry while executing npm audit check."

            // Retry npm audit in case it failed
            final int MAX_RETRIES = 4
            int retryCount = 1
            int waitTimeInSeconds = 10
            boolean hasSucceeded = false
            while (retryCount <= MAX_RETRIES && (!hasSucceeded)) {

                sh script: "npm audit --json --registry=https://registry.npmjs.org > npm-audit.json", returnStatus: true
                Map npmAuditResult = readJSON file: "npm-audit.json"

                if (npmAuditResult.containsKey("advisories")) {
                    discoveredAdvisories = npmAuditResult.advisories
                    hasSucceeded = true
                } else {
                    String npmAuditRegistryNotReachableErrorMessage = "Failed to parse the scan results of npm audit. " +
                        "It might be that the npm registry did not respond as expected, or that it is not reachable. " +
                        "Please check for additional log messages in the npm audit stage."

                    if (retryCount == MAX_RETRIES) {
                        error npmAuditRegistryNotReachableErrorMessage + " Won't retry audit anymore."
                    } else {
                        echo npmAuditRegistryNotReachableErrorMessage + " Will retry to run npm audit."
                        sleep time: waitTimeInSeconds, unit: 'SECONDS'
                    }
                }
                retryCount++
                waitTimeInSeconds += 30
            }
        }

        Map advisories = filterUserAuditedAdvisories(configuration, discoveredAdvisories)

        if (advisories == null) {
            error("npm audit was not successful, expected 'advisories' not to be null, but it is null. " +
                "This should not happen, if it does, please open an issue at https://github.com/sap/cloud-s4-sdk-pipeline/issues and describe in detail what happened.")
        }

        Map criticalAdvisories = advisories.findAll { it.value.severity == 'critical' }
        Map highAdvisories = advisories.findAll { it.value.severity == 'high' }
        Map moderateAdvisories = advisories.findAll { it.value.severity == 'moderate' }

        Map vulnerabilitySummary = [
            critical: criticalAdvisories.size(),
            high    : highAdvisories.size(),
            moderate: moderateAdvisories.size()
        ]

        if (vulnerabilitySummary.critical > 0 || vulnerabilitySummary.high > 0 || vulnerabilitySummary.moderate > 2) {
            script.currentBuild.setResult('FAILURE')

            String summary = "npm dependency audit discovered ${vulnerabilitySummary.critical} crticial, ${vulnerabilitySummary.high} high and ${vulnerabilitySummary.moderate} moderate vulnerabilities in '${basePath}'"

            String npmAuditSummary = "${summary}.\n" +
                "Please execute 'npm audit' locally to identify and fix relevant findings.\n" +
                "Summary of the findings:\n" +
                "${formatRelevantAdvisoriesForLog(criticalAdvisories, highAdvisories, moderateAdvisories)}"

            addBadge(icon: "error.gif", text: npmAuditSummary)

            createSummary(icon: "error.gif", text: "<h2>npm dependency audit discovered ${vulnerabilitySummary.critical} crticial and ${vulnerabilitySummary.high} high and ${vulnerabilitySummary.moderate} moderate vulnerabilities</h2>\n" +
                "Please execute <code>npm audit</code> locally to identify and fix relevant findings.\n" +
                "<h3>Summary of the findings</h3>\n" + formatRelevantAdvisoriesForBadge(criticalAdvisories, highAdvisories, moderateAdvisories))

            error npmAuditSummary
        }
    }
}

private Map filterUserAuditedAdvisories(Map configuration, Map advisories) {
    List userAuditedAdvisories = configuration?.auditedAdvisories

    if (userAuditedAdvisories && advisories) {
        // Handle non-string list elements
        userAuditedAdvisories = userAuditedAdvisories.collect { String.valueOf(it) }

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

@NonCPS
private String htmlListOfUserAuditedAdvisories(List userAuditedAdvisories) {
    return "<ol>${userAuditedAdvisories.collect { "<li><a target=\"_blank\" href=\"https://npmjs.com/advisories/${it}\">${it}</a></li>" }.join("\n")}</ol>"
}

@NonCPS
private String formatRelevantAdvisoriesForBadge(Map critical, Map high, Map moderate) {
    List criticalList = critical.collect { advisoryId, advisoryBody -> formatHtml(advisoryBody) }
    List highList = high.collect { advisoryId, advisoryBody -> formatHtml(advisoryBody) }
    List moderateList = moderate.collect { advisoryId, advisoryBody -> formatHtml(advisoryBody) }

    return (criticalList + highList + moderateList).collect { "<li>${it}</li>" }?.join('\n')
}

@NonCPS
private String formatHtml(def advisoryBody) {
    return "${severity(advisoryBody)} <em>${advisoryBody.title}</em> vulnerability found in dependency \"${advisoryBody.module_name}\", " +
        "see <a target=\"_blank\" href=\"${advisoryBody.url}\">${advisoryBody.url}</a> for details."
}

@NonCPS
private String formatRelevantAdvisoriesForLog(Map critical, Map high, Map moderate) {
    List criticalList = critical.collect { advisoryId, advisoryBody -> format(advisoryBody) }
    List highList = high.collect { advisoryId, advisoryBody -> format(advisoryBody) }
    List moderateList = moderate.collect { advisoryId, advisoryBody -> format(advisoryBody) }

    return (criticalList + highList + moderateList).join('\n')
}

@NonCPS
private String format(def advisoryBody) {
    return "${severity(advisoryBody)} \"${advisoryBody.title}\" vulnerability found in dependency \"${advisoryBody.module_name}\", see ${advisoryBody.url} for details."
}

@NonCPS
private String severity(def advisoryBody) {
    return (advisoryBody.severity).capitalize()
}
