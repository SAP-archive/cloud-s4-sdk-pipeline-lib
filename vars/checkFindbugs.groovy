import com.sap.icd.jenkins.ConfigurationLoader
import com.sap.icd.jenkins.ConfigurationMerger

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'checkFindbugs', stepParameters: parameters) {
        def script = parameters.script

        def stepDefaults = [includeFilterFile: 's4hana_findbugs_include_filter.xml']

        Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, 'checkFindbugs')

        List parameterKeys = [
            'dockerImage',
            'excludeFilterFile'
        ]
        List stepConfigurationKeys = [
            'dockerImage',
            'excludeFilterFile'
        ]
        Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)

        def filterOptions = ''

        def excludeFilterFile = configuration.excludeFilterFile
        if (excludeFilterFile?.trim() && fileExists(excludeFilterFile)) {
            filterOptions += "-Dfindbugs.excludeFilterFile=${excludeFilterFile} "
        }

        def includeFilterFile = configuration.includeFilterFile
        def localIncludeFilerPath = "s4hana_pipeline/${includeFilterFile}"
        writeFile file: localIncludeFilerPath, text: libraryResource(includeFilterFile)
        filterOptions += "-Dfindbugs.includeFilterFile=${localIncludeFilerPath}"

        executeMaven script: script, flags: '-B -U', m2Path: s4SdkGlobals.m2Directory, goals: 'findbugs:findbugs', defines: filterOptions, dockerImage: configuration.dockerImage
        findbugs canComputeNew: false, excludePattern: excludeFilterFile, failedTotalHigh: '0', failedTotalNormal: '10', includePattern: includeFilterFile, pattern: '**/findbugsXml.xml'

        if (fileExists("application/target/findbugsXml.xml")) {
            dir(s4SdkGlobals.reportsDirectory){ sh "cp -p ${workspace}/application/target/findbugsXml.xml ." }
        }
    }
}
