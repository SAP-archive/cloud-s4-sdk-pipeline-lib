package com.sap.cloud.sdk.s4hana.pipeline

import org.junit.Before
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class NeoDeployCommandHelperTest {

    private NeoDeployCommandHelper commandHelper

    @Before
    void setUp() throws Exception {
        Map deploymentDescriptor = [
            host          : 'host_value',
            account       : 'account_value',
            application   : 'application_value',
            ev            : ['ENV1=value1', 'ENV2=value2'],
            vmArguments   : '-Dargument1=value1 -Dargument2=value2',
            runtime       : 'neо-javaee6-wp',
            runtimeVersion: '2',
            size          : 'lite'
        ]
        String username = 'username'
        String password = 'password'
        String source = 'file.war'

        commandHelper = new NeoDeployCommandHelper(deploymentDescriptor, username, password, source)
    }

    @Test
    void testAssertMandatoryParameters() {
        def failingCommandHelper = new NeoDeployCommandHelper([:], "u", "p", "source")
        shouldFail { failingCommandHelper.assertMandatoryParameters() }
    }

    @Test
    void testStatusCommand() {
        String actual = commandHelper.statusCommand()
        String expected = "/sdk/tools/neo.sh status -h host_value -a account_value -b application_value -u username -p password"
        assertEquals(expected, actual)
    }

    @Test
    void testRollingUpdateCommand() {
        String actual = commandHelper.rollingUpdateCommand()
        String basicCommand = "/sdk/tools/neo.sh rolling-update -h host_value -a account_value -b application_value -u username -p password"
        basicCommand += ' -s file.war'

        assertTrue(actual.contains(basicCommand))
        assertTrue(actual.contains(' --ev ENV1=value1 --ev ENV2=value2'))
        assertTrue(actual.contains(' --vm-arguments "-Dargument1=value1 -Dargument2=value2"'))
        assertTrue(actual.contains('--runtime neо-javaee6-wp'))
        assertTrue(actual.contains(' --runtime-version 2'))
        assertTrue(actual.contains(' --size lite'))
    }

    @Test
    void testDeployCommand() {
        String actual = commandHelper.deployCommand()
        String basicCommand = "/sdk/tools/neo.sh deploy -h host_value -a account_value -b application_value -u username -p password"
        basicCommand += ' -s file.war'

        assertTrue(actual.contains(basicCommand))
        assertTrue(actual.contains(' --ev ENV1=value1 --ev ENV2=value2'))
        assertTrue(actual.contains(' --vm-arguments "-Dargument1=value1 -Dargument2=value2"'))
        assertTrue(actual.contains(' --runtime neо-javaee6-wp'))
        assertTrue(actual.contains(' --runtime-version 2'))
        assertTrue(actual.contains(' --size lite'))
    }

    @Test
    void testRestartCommand() {
        String actual = commandHelper.restartCommand()
        String expected = "/sdk/tools/neo.sh restart --synchronous -h host_value -a account_value -b application_value -u username -p password"
        assertEquals(expected, actual)
    }
}
