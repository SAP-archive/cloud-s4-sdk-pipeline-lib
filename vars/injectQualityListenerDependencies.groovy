import com.sap.cloud.sdk.s4hana.pipeline.MavenUtils
import com.sap.cloud.sdk.s4hana.pipeline.PathUtils

import groovy.util.Node
import groovy.util.XmlParser
import groovy.xml.XmlUtil

def call(Map parameters = [:]) {

    def script = parameters.script
    String basePath = parameters.basePath

    String dependencyTree = MavenUtils.getMavenDependencyTree(script, basePath)
    String connectivityDependency = getConnectivityDependency(dependencyTree)

    if (connectivityDependency) {
        String sdkIdentifier = 'sdk'
        String version = ''
        version = connectivityDependency.split(':')[3] // Pattern: groupId:artifactId:packaging:version:scope

        sdkIdentifier = getSdkIdentifier(connectivityDependency)
        String groupId = 'com.sap.cloud.' + sdkIdentifier + '.quality'
        String artifactId = 'listeners-all'
        String scope = 'test'

        String odataPattern = groupId + ':odata-querylistener:jar:' + version + ':test'
        String rfcPattern = groupId + ':rfc-querylistener:jar:' + version + ':test'
        String httpclientPattern = groupId + ':httpclient-listener:jar:'  + version + ':test'

        if (!(dependencyTree.contains(odataPattern)) || !(dependencyTree.contains(rfcPattern)) || !(dependencyTree.contains(httpclientPattern))) {
            echo "One of the following dependencies were not found in the pom.xml at $basePath: \n \n \t $odataPattern \n \t $rfcPattern \n \t $httpclientPattern \n\n" +
                "Those depedencies will be substituted with '$groupId:$artifactId:$version:$scope' which will be used in the unit- and integrationtests."

            injectListenersAndWritePom(groupId, artifactId, version, scope, PathUtils.normalize(basePath, 'pom.xml'))
        }
    }
}

private String getConnectivityDependency(String dependencyTree) {
    return dependencyTree.split('\n').find() {
        (it.contains('cloudplatform:connectivity:jar') || it.contains('cloudplatform:cloudplatform-connectivity:jar'))
    }
}

private String getSdkIdentifier(String connectivityDependency) {
    if (connectivityDependency.contains('s4hana')) {
        return 's4hana'
    } else {
        return 'sdk'
    }
}

private void injectListenersAndWritePom(String groupId, String artifactId, String version, String scope, String pathToPom) {
    String pomXml = readFile file: pathToPom

    def xml = new XmlParser().parseText(pomXml)
    def dependency = xml.dependencies[0].appendNode('dependency')
    dependency.appendNode('groupId', groupId)
    dependency.appendNode('artifactId', artifactId)
    dependency.appendNode('version', version)
    dependency.appendNode('scope', scope)

    writeFile text: XmlUtil.serialize(xml), file: pathToPom
}
