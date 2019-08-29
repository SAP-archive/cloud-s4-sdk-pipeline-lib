import com.lesfurets.jenkins.unit.BasePipelineTest
import com.sap.cloud.sdk.s4hana.pipeline.MavenUtils
import com.sap.cloud.sdk.s4hana.pipeline.mock.NullScript
import com.sap.cloud.sdk.s4hana.pipeline.util.BaseCloudSdkTest
import groovy.xml.XmlUtil
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class injectQualityListenerDependenciesTest extends BaseCloudSdkTest {

    @Before
    void prepareTests() throws Exception {
        setUp()
    }

    @Test
    void 'Nothing should happen if all listeners are already in pom.xml'() {
        String dependencyTree = """sap:GettingStartedBookshop-srv:war:1.0-SNAPSHOT
+- com.fasterxml.jackson.core:jackson-databind:jar:2.9.9.2:compile
|  +- com.fasterxml.jackson.core:jackson-annotations:jar:2.9.8:compile
|  \\- com.fasterxml.jackson.core:jackson-core:jar:2.9.8:compile
+- com.sap.cloud.s4hana.quality:listeners-all:jar:2.8.1:test
|  +- com.sap.cloud.s4hana.quality:odata-querylistener:jar:2.8.1:test
|  |  +- com.google.code.findbugs:jsr305:jar:3.0.2:compile
|  |  \\- com.sap.cloud.s4hana.quality:common:jar:2.8.1:compile
|  +- com.sap.cloud.s4hana.quality:rfc-querylistener:jar:2.8.1:test
|  \\- com.sap.cloud.s4hana.quality:httpclient-listener:jar:2.8.1:test
|     \\- com.sap.cloud.s4hana.cloudplatform:connectivity:jar:2.8.1:compile"""

        MavenUtils.metaClass.static.getMavenDependencyTree = { Script script, String basePath -> dependencyTree }
        boolean pomModified = false

        helper.registerAllowedMethod('readFile', [Map.class], { Map filePath ->
            pomModified = true
        })
        helper.registerAllowedMethod('writeFile', [Map.class], { Map parameters ->
            pomModified = true
        })

        Script script = loadScript("vars/injectQualityListenerDependencies.groovy")
        script.invokeMethod("call", [script: dummyScript, basePath: './'])
        assertFalse(pomModified)
    }

    @Test
    void 'On missing httpclient-listener dependency listeners-all should be written to pom.xml for SDK v2'() {
        String dependencyTree = """sap:GettingStartedBookshop-srv:war:1.0-SNAPSHOT
+- com.fasterxml.jackson.core:jackson-databind:jar:2.9.9.2:compile
|  +- com.fasterxml.jackson.core:jackson-annotations:jar:2.9.8:compile
|  \\- com.fasterxml.jackson.core:jackson-core:jar:2.9.8:compile
+- com.sap.cloud.s4hana.quality:listeners-all:jar:2.8.1:test
|  +- com.sap.cloud.s4hana.quality:odata-querylistener:jar:2.8.1:test
|  |  +- com.google.code.findbugs:jsr305:jar:3.0.2:compile
|  |  \\- com.sap.cloud.s4hana.quality:common:jar:2.8.1:compile
|  +- com.sap.cloud.s4hana.quality:rfc-querylistener:jar:2.8.1:test
|     \\- com.sap.cloud.s4hana.cloudplatform:connectivity:jar:2.8.1:compile"""

        String pomXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <artifactId>GettingStartedBookshop</artifactId>
  <groupId>sap</groupId>
  <version>1.0-SNAPSHOT</version>
  <packaging>pom</packaging>
  <name>GettingStartedBookshop</name>  
  <dependencies>
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <scope>test</scope>
    </dependency>
  </dependencies>
</project>"""
        MavenUtils.metaClass.static.getMavenDependencyTree = { Script script, String basePath -> dependencyTree }
        String modifiedPom = ''
        helper.registerAllowedMethod('readFile', [Map.class], { Map filePath ->
            pomXml
        })
        helper.registerAllowedMethod('writeFile', [Map.class], { Map parameters ->
            modifiedPom = parameters.text
        })

        Script script = loadScript("vars/injectQualityListenerDependencies.groovy")
        script.invokeMethod("call", [script: dummyScript, basePath: './'])

        Node dependencies = new XmlParser().parseText(modifiedPom).dependencies[0]
        Node addedDependency = dependencies.getDirectChildren().get(dependencies.children().size()-1)

        assertTrue(XmlUtil.serialize(addedDependency).contains("<dependency xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
            "  <groupId>com.sap.cloud.s4hana.quality</groupId>\n" +
            "  <artifactId>listeners-all</artifactId>\n" +
            "  <version>2.8.1</version>\n" +
            "  <scope>test</scope>\n" +
            "</dependency>"))
    }

    @Test
    void 'On missing httpclient-listener dependency listeners-all should be written to pom.xml for SDK v3'() {
        String dependencyTree = """sap:timesheet-srv:war:1.0-SNAPSHOT
+- com.sap.cloud.sdk.quality:listeners-all:jar:3.0.0:test
|  +- com.sap.cloud.sdk.quality:odata-querylistener:jar:3.0.0:test
|  |  +- com.google.code.findbugs:jsr305:jar:3.0.2:compile
|  |  +- com.sap.cloud.sdk.quality:common:jar:3.0.0:compile
|  |  \\- com.google.errorprone:error_prone_annotations:jar:2.3.3:compile
|  +- com.sap.cloud.sdk.quality:rfc-querylistener:jar:3.0.0:test
|     \\- com.sap.cloud.sdk.cloudplatform:cloudplatform-connectivity:jar:3.0.0:compile"""

        String pomXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <artifactId>GettingStartedBookshop</artifactId>
  <groupId>sap</groupId>
  <version>1.0-SNAPSHOT</version>
  <packaging>pom</packaging>
  <name>GettingStartedBookshop</name>  
  <dependencies>
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <scope>test</scope>
    </dependency>
  </dependencies>
</project>"""
        MavenUtils.metaClass.static.getMavenDependencyTree = { Script script, String basePath -> dependencyTree }
        String modifiedPom = ''
        helper.registerAllowedMethod('readFile', [Map.class], { Map filePath ->
            pomXml
        })
        helper.registerAllowedMethod('writeFile', [Map.class], { Map parameters ->
            modifiedPom = parameters.text
        })

        Script script = loadScript("vars/injectQualityListenerDependencies.groovy")
        script.invokeMethod("call", [script: dummyScript, basePath: './'])

        Node dependencies = new XmlParser().parseText(modifiedPom).dependencies[0]
        Node addedDependency = dependencies.getDirectChildren().get(dependencies.children().size()-1)

        assertTrue(XmlUtil.serialize(addedDependency).contains("<dependency xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
            "  <groupId>com.sap.cloud.sdk.quality</groupId>\n" +
            "  <artifactId>listeners-all</artifactId>\n" +
            "  <version>3.0.0</version>\n" +
            "  <scope>test</scope>\n" +
            "</dependency>"))
    }
}
