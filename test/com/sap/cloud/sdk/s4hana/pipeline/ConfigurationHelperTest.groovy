package com.sap.cloud.sdk.s4hana.pipeline

import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertEquals

class ConfigurationHelperTest {

    private static getConfiguration() {
        Map configuration = [dockerImage: 'maven:3.2-jdk-8-onbuild']
        return configuration
    }

    @Test
    void testGetProperty() {
        def configuration = new ConfigurationHelper(getConfiguration())
        assertEquals('maven:3.2-jdk-8-onbuild', configuration.getConfigProperty('dockerImage'))
        assertEquals('default', configuration.getConfigProperty('something', 'default'))
        assertTrue(configuration.isPropertyDefined('dockerImage'))
        assertFalse(configuration.isPropertyDefined('something'))
    }

    @Test
    void testIsPropertyDefined() {
        def configuration = new ConfigurationHelper(getConfiguration())
        assertTrue(configuration.isPropertyDefined('dockerImage'))
        assertFalse(configuration.isPropertyDefined('something'))
    }

    @Test
    void testGetMandatoryProperty() {
        def configuration = new ConfigurationHelper(getConfiguration())
        assertEquals('maven:3.2-jdk-8-onbuild', configuration.getMandatoryProperty('dockerImage'))
        assertEquals('default', configuration.getMandatoryProperty('something', 'default'))

        shouldFail { configuration.getMandatoryProperty('something') }
    }
}
