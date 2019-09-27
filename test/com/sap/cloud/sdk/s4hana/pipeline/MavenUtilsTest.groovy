package com.sap.cloud.sdk.s4hana.pipeline

import com.sap.cloud.sdk.s4hana.pipeline.mock.NullScript
import com.sap.cloud.sdk.s4hana.pipeline.util.BaseCloudSdkTest
import static org.junit.Assert.*
import static org.hamcrest.CoreMatchers.*

import org.junit.Before
import org.junit.Test

class MavenUtilsTest extends BaseCloudSdkTest {

    List mavenExecuteCalls = []
    @Before
    void prepareTests() throws Exception {
        setUp()
        dummyScript = new NullScript(this)

        helper.registerAllowedMethod("mavenExecute", [Map.class], { parameters ->
            mavenExecuteCalls.push(parameters)
        })
    }

    @Test
    void 'Install artifacts for packaging type pom'() {
        Map pom = [
            artifactId: 'service',
            packaging: 'pom'
        ]

        BuildToolEnvironment.instance.setBuildTool(BuildTool.MTA)

        MavenUtils.installMavenArtifacts(dummyScript, pom, 'srv', 'srv/pom.xml')

        assertEquals(mavenExecuteCalls.size(), 1)


        String normalizedFilePath = PathUtils.normalize('srv', 'pom.xml')
        assertThat(mavenExecuteCalls[0].defines, containsString("-Dfile=${normalizedFilePath}"));
        assertThat(mavenExecuteCalls[0].defines, containsString("-DpomFile=srv/pom.xml"));
    }

    @Test
    void 'Install artifacts for packaging type war'() {
        Map pom = [
            artifactId: 'service',
            packaging: 'war'
        ]

        BuildToolEnvironment.instance.setBuildTool(BuildTool.MTA)

        helper.registerAllowedMethod("findFiles", [Map.class], { parameters ->
            if(parameters.glob.endsWith("target/service*-classes.jar")){
                return [new FileWrapperMock("srv/target/service-classes.jar")]
            }
            else if(parameters.glob.endsWith("target/service*.war")){
                return [new FileWrapperMock("srv/target/service.war")]
            }
            else {
                throw new RuntimeException("Glob '${parameters.glob}' not expected!")
            }
        })

        MavenUtils.installMavenArtifacts(dummyScript, pom, 'srv', 'srv/pom.xml')

        assertEquals(mavenExecuteCalls.size(), 2)
        assertThat(mavenExecuteCalls[0].defines, containsString('-Dfile=srv/target/service-classes.jar'));
        assertThat(mavenExecuteCalls[0].defines, containsString('-DpomFile=srv/pom.xml'));

        assertThat(mavenExecuteCalls[1].defines, containsString('-Dfile=srv/target/service.war'));
        assertThat(mavenExecuteCalls[1].defines, containsString('-DpomFile=srv/pom.xml'));
    }

    @Test
    void 'Install artifacts for packaging type jar'() {
        Map pom = [
            artifactId: 'service',
            packaging: 'jar'
        ]

        BuildToolEnvironment.instance.setBuildTool(BuildTool.MTA)

        helper.registerAllowedMethod("findFiles", [Map.class], { parameters ->
            if(parameters.glob.endsWith("target/service*.jar")){
                return [new FileWrapperMock("srv/target/service.jar")]
            }
            else {
                throw new RuntimeException("Glob '${parameters.glob}' not expected!")
            }
        })

        MavenUtils.installMavenArtifacts(dummyScript, pom, 'srv', 'srv/pom.xml')

        assertEquals(mavenExecuteCalls.size(), 1)
        assertThat(mavenExecuteCalls[0].defines, containsString('-Dfile=srv/target/service.jar'));
        assertThat(mavenExecuteCalls[0].defines, containsString('-DpomFile=srv/pom.xml'));
    }
}

class FileWrapperMock {
    FileWrapperMock(String path){
        this.path = path
    }

    String path
}
