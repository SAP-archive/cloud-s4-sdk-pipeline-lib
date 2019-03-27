import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils

import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

def call(Map parameters){
    Script script = parameters.script
    List customScripts = parameters.customScripts ?: []
    def dockerOptions = ['--cap-add=SYS_ADMIN']
    DownloadCacheUtils.appendDownloadCacheNetworkOption(script, dockerOptions)
    if (fileExists('package-lock.json') || fileExists('npm-shrinkwrap.json')) {
        executeNpm(script: script, dockerOptions: dockerOptions) {
            sh "npm ci"
            executeCustomNpmScripts(customScripts)
        }
    } else {
        executeNpm(script: script, dockerOptions: dockerOptions) {
            warnAboutMissingPackageLock()
            sh "npm install"
            executeCustomNpmScripts(customScripts)
        }
    }
}

private void warnAboutMissingPackageLock() {
    String noPackageLockWarningText = "Found a package.json file, but no package lock file in your project. " +
        "It is recommended to create a `package-lock.json` file by running `npm install` locally and to add this file to your version control. " +
        "By doing so, the builds of your application are more reliable."
    echo noPackageLockWarningText
    assertPluginIsActive('badge')
    addBadge(icon: "warning.gif", text: noPackageLockWarningText)
    createSummary(icon: "warning.gif", text: "<h2>No npm package lock file found</h2>\n" +
        "Found a <code>package.json</code> file, but no package lock file in your project.\n" +
        "It is recommended to create a <code>package-lock.json</code> file by running <code>npm install</code> locally and to add this file to your version control. " +
        "By doing so, the builds of your application are more reliable.")
}

private void executeCustomNpmScripts(List customScripts = []) {
    customScripts.each {
        npmScript -> sh "npm run ${npmScript}"
    }
}
