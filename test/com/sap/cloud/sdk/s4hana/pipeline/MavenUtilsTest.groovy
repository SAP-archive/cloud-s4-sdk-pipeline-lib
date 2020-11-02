package com.sap.cloud.sdk.s4hana.pipeline

import com.sap.cloud.sdk.s4hana.pipeline.mock.NullScript
import com.sap.cloud.sdk.s4hana.pipeline.util.BaseCloudSdkTest

import static org.junit.Assert.*
import static org.hamcrest.CoreMatchers.*

import org.junit.Before
import org.junit.Test

class MavenUtilsTest extends BaseCloudSdkTest {

    List mavenExecuteCalls = []
    List existingFiles = []
    Map pomFiles = [:]

    @Before
    void prepareTests() throws Exception {
        setUp()
        dummyScript = new NullScript(this)

        helper.registerAllowedMethod("mavenExecute", [Map.class], { parameters ->
            mavenExecuteCalls.push(parameters)
        })

        helper.registerAllowedMethod("fileExists", [String.class], { filePath ->
            return existingFiles.contains(filePath)
        })

        helper.registerAllowedMethod("readMavenPom", [Map.class], { parameters ->
            String file = parameters.file

            if(!pomFiles[file]) {
                throw new FileNotFoundException(file, "File not found")
            }

            return pomFiles[file]
        })
    }

    private void registerPomFile(String fileName, Map content){
        existingFiles.push(fileName)
        pomFiles[fileName] = content
    }

    @Test
    void 'Install artifacts for packaging type pom'() {
        registerPomFile('srv/pom.xml', [
            artifactId: 'service',
            packaging: 'pom'
        ])

        MavenUtils.installMavenArtifacts(dummyScript, 'srv')

        assertEquals(mavenExecuteCalls.size(), 1)

        def wants1 = '-Dfile=srv/pom.xml'
        assertThat(mavenExecuteCalls[0].defines, hasItem("${wants1}"))
        def wants2 = "-DpomFile=srv/pom.xml"
        assertThat(mavenExecuteCalls[0].defines, hasItem("${wants2}"))
    }

    @Test
    void 'Install artifacts for packaging type pom with flattened pom'() {
        registerPomFile('srv/pom.xml', [
            artifactId: 'service',
            packaging: 'pom'
        ])

        registerPomFile('srv/.flattened-pom.xml', [
            artifactId: 'service',
            packaging: 'pom'
        ])

        MavenUtils.installMavenArtifacts(dummyScript, 'srv')

        assertEquals(mavenExecuteCalls.size(), 1)

        def wants1 = "-Dfile=srv/.flattened-pom.xml"
        assertThat(mavenExecuteCalls[0].defines, hasItem("${wants1}"))
        def wants2 = "-DpomFile=srv/.flattened-pom.xml"
        assertThat(mavenExecuteCalls[0].defines, hasItem("${wants2}"))
    }

    @Test
    void 'Install artifacts for packaging type war'() {

        registerPomFile('srv/pom.xml', [
            artifactId: 'service',
            packaging: 'war'
        ])

        helper.registerAllowedMethod("findFiles", [Map.class], { parameters ->
            if(parameters.glob.endsWith("target/service*-classes.jar")){
                return [new FileWrapperMock("srv/target/service-classes.jar")]
            }
            else if(parameters.glob.endsWith("target/service*.war")){
                return [new FileWrapperMock("srv/target/service.war")]
            }
            throw new RuntimeException("Glob '${parameters.glob}' not expected!")
        })

        MavenUtils.installMavenArtifacts(dummyScript, 'srv')

        assertEquals(mavenExecuteCalls.size(), 2)

        def wants1 = '-Dfile=srv/target/service.war'
        assertThat(mavenExecuteCalls[0].defines, hasItem("${wants1}"))
        def wants2 = '-DpomFile=srv/pom.xml'
        assertThat(mavenExecuteCalls[0].defines, hasItem("${wants2}"))

        def wants3 = '-Dfile=srv/target/service-classes.jar'
        assertThat(mavenExecuteCalls[1].defines, hasItem("${wants3}"))

    }

    @Test
    void 'Install artifacts for packaging type war with flattened pom'() {

        registerPomFile('srv/pom.xml', [
            artifactId: 'service',
            packaging: 'war'
        ])

        registerPomFile('srv/.flattened-pom.xml', [
            artifactId: 'service',
            packaging: 'war'
        ])

        helper.registerAllowedMethod("findFiles", [Map.class], { parameters ->
            if(parameters.glob.endsWith("target/service*-classes.jar")){
                return [new FileWrapperMock("srv/target/service-classes.jar")]
            }
            else if(parameters.glob.endsWith("target/service*.war")){
                return [new FileWrapperMock("srv/target/service.war")]
            }
            throw new RuntimeException("Glob '${parameters.glob}' not expected!")
        })

        MavenUtils.installMavenArtifacts(dummyScript, 'srv')

        assertEquals(mavenExecuteCalls.size(), 2)

        def wants = '-DpomFile=srv/.flattened-pom.xml'
        assertThat(mavenExecuteCalls[0].defines, hasItem("${wants}"))
        assertThat(mavenExecuteCalls[1].defines, hasItem("${wants}"))
    }

    @Test
    void 'Install artifacts for packaging type jar including classes'() {
        registerPomFile('srv/pom.xml', [
            artifactId: 'service',
            packaging: 'jar'
        ])

        helper.registerAllowedMethod("findFiles", [Map.class], { parameters ->
            if(parameters.glob.endsWith("target/service*.jar")){
                return [new FileWrapperMock("srv/target/service.jar")]
            }
            if (parameters.glob.endsWith("target/service*-classes.jar")) {
                return [new FileWrapperMock("srv/target/service-classes.jar")]
            }
            throw new RuntimeException("Glob '${parameters.glob}' not expected!")
        })

        MavenUtils.installMavenArtifacts(dummyScript, 'srv')

        assertEquals(mavenExecuteCalls.size(), 2)

        assertArrayEquals(['-Dfile=srv/target/service.jar', '-DpomFile=srv/pom.xml'] as Object[], mavenExecuteCalls[0].defines  as Object[])
        assertArrayEquals(['-Dfile=srv/target/service-classes.jar', '-DpomFile=srv/pom.xml', '-Dpackaging=jar', '-Dclassifier=classes'] as Object[], mavenExecuteCalls[1].defines  as Object[])
    }
}

class FileWrapperMock {
    FileWrapperMock(String path){
        this.path = path
    }

    String path
}
