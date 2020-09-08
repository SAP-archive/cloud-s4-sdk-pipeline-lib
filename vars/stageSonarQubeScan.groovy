import com.cloudbees.groovy.cps.NonCPS
import com.sap.piper.ConfigurationLoader

import java.nio.file.Paths

def call(Map parameters = [:]) {

    String stageName = 'sonarQubeScan'
    Script script = parameters.script
    piperStageWrapper(stageName: stageName, script: script) {

        Map stageConfiguration = loadEffectiveStageConfiguration(script: script, stageName: stageName)
        List jacocoReportPaths = getJacocoReportPaths().collect({ it.path })

        List sonarProperties = [
            "sonar.java.binaries=${getPathToBinaries().join(',')}".toString(),
            "sonar.coverage.exclusions=**.js,unit-tests/**,integration-tests/**,performance-tests/**,**.xml,**/target/**",
            "sonar.jacoco.reportPaths=${jacocoReportPaths.join(',')}".toString(),
            'sonar.java.libraries=./s4hana_pipeline/maven_local_repo/**'
        ]

        if (stageConfiguration?.sonarProperties) {
            sonarProperties.addAll(stageConfiguration.sonarProperties)
        }

        if (!stageConfiguration?.projectKey) {
            error("Configure the option 'projectKey' in your stage configuration for 'sonarQubeScan'")
        }

        sonarProperties.add("sonar.projectKey=${stageConfiguration.projectKey}".toString())

        if (!stageConfiguration?.instance) {
            error("Configure the option 'instance' in your stage configuration for 'sonarQubeScan'")
        }

        if (!stageConfiguration.runInAllBranches && !isProductiveBranch(script: script)) {
            return
        }

        if (stageConfiguration.runInAllBranches && !isProductiveBranch(script: script) && !isPullRequest()) {
            sonarExecuteScan([
                script     : script,
                dockerImage: stageConfiguration.dockerImage ?: 'ppiper/node-browsers:v3',
                instance   : stageConfiguration.instance,
                options    : sonarProperties,
                branchName : env.BRANCH_NAME
            ])
        } else {
            sonarExecuteScan([
                script     : script,
                dockerImage: stageConfiguration.dockerImage ?: 'ppiper/node-browsers:v3',
                instance   : stageConfiguration.instance,
                options    : sonarProperties
            ])
        }
    }
}

private List getJacocoReportPaths() {
    return findFiles(glob: "**/target/**/*.exec")
}

private List getPathToBinaries() {
    def pomFiles = findFiles(glob: "**/pom.xml")
    List binaries = []

    for (def pomFile : pomFiles) {
        String mavenModule = getParentFolder(pomFile.path)
        String classesPath = "$mavenModule/target/classes/"

        if (fileExists(classesPath)) {
            binaries.push(classesPath)
        }
    }
    return binaries
}

@NonCPS
String getParentFolder(String filePath) {
    return Paths.get(filePath).getParent().toString()
}

private Boolean isPullRequest() {
    return env.CHANGE_ID
}
