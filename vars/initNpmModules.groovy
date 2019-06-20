import com.cloudbees.groovy.cps.NonCPS
import com.sap.cloud.sdk.s4hana.pipeline.BuildToolEnvironment
import com.sap.cloud.sdk.s4hana.pipeline.NpmModule

import java.nio.file.Path
import java.nio.file.Paths

def call (Map parameters = [:]) {

    String basePath
    List npmModules = []

    def files = findFiles(glob: '**/package.json', excludes: '**/node_modules/**')

    for (int i = 0; i < files.size(); i++) {
        String packageJsonPath = (String) files[i].path
        basePath = getBasePath(packageJsonPath)
        Map packageJson = readJSON file: packageJsonPath

        List npmScripts = packageJson.scripts.keySet() as List
        npmModules.add(new NpmModule(basePath: basePath, npmScripts: npmScripts))
    }

    BuildToolEnvironment.instance.npmModules = npmModules
}

@NonCPS
private String getBasePath(String packageJsonPath) {
    String basePath
    if (packageJsonPath == 'package.json') {
        basePath = '.'
    } else {
        Path pathToPackageJson = Paths.get(packageJsonPath)
        basePath = pathToPackageJson.getParent().toString()
    }
    return basePath
}
