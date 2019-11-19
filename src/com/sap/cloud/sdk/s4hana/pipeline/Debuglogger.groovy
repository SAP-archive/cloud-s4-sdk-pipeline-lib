package com.sap.cloud.sdk.s4hana.pipeline

import com.cloudbees.groovy.cps.NonCPS
import groovy.text.SimpleTemplateEngine

@Singleton
class Debuglogger {
    String fileName
    String projectIdentifier = null
    Map environment = ["environment": "custom"]
    String buildTool = null
    Map modulesMap = [:]
    List npmModules = []
    Set plugins = []
    Map github = [:]
    Map localExtensions = [:]
    String globalExtensionRepository = null
    Map globalExtensions = [:]
    String globalExtensionConfigurationFilePath = null
    String sharedConfigFilePath = null
    Set additionalSharedLibraries = []
    Map failedBuild = [:]
    boolean shareConfidentialInformation

    def generateReport(Script script) {
        String template
        template = script.libraryResource "debug_log.txt"

        if (!projectIdentifier) {
            this.projectIdentifier = "NOT_SET"
        }

        Jenkins.instance.getPluginManager().getPlugins().each {
            this.plugins.add("${it.getShortName()} | ${it.getVersion()} | ${it.getDisplayName()}")
        }

        Map binding = getProperties()
        Date now = new Date()

        binding.utcTimestamp = now.format("yyyy-MM-dd HH:mm", TimeZone.getTimeZone('UTC'))
        String fileNameTimestamp = now.format("yyyy-MM-dd-HH-mm", TimeZone.getTimeZone('UTC'))

        if (this.shareConfidentialInformation) {
            fileName = "confidential_debug_log_${fileNameTimestamp}_${projectIdentifier}.txt"
        } else {
            fileName = "redacted_debug_log_${fileNameTimestamp}_${projectIdentifier}.txt"
        }

        return fillTemplate(template, binding)
    }

    @NonCPS
    private String fillTemplate(String template, binding) {
        def engine = new SimpleTemplateEngine()
        return engine.createTemplate(template).make(binding)
    }
}
