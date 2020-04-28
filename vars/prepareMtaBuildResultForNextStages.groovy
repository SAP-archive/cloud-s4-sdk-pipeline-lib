import com.sap.cloud.sdk.s4hana.pipeline.MavenUtils
import com.sap.cloud.sdk.s4hana.pipeline.MtaUtils

def call(Map parameters) {
    Script script = parameters.script

    // The MTA builder executes the maven command only inside the java module directories and not on the root directory.
    // Hence we need install root pom to local repository after the project is built using the mta builder
    if (fileExists('pom.xml')) {
        MavenUtils.flattenPomXmls(script)
        MavenUtils.installRootPom(script)
    }

    // install maven artifacts in local maven repo because `mbt build` executes `mvn package -B`
    MtaUtils.installAllMavenModules(script)

    // mta-builder executes 'npm install --production', therefore we need 'npm ci/install' to install the dev-dependencies
    npmExecuteScripts(script: script, runScripts: [], install: true)
}
