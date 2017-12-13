package com.sap.icd.jenkins

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull

class ConfigurationMergerTest {

    @Test
    void testMerge(){
        Map defaults = [dockerImage: 'mvn']
        Map parameters = [goals: 'install', flags: '']
        List parameterKeys = ['flags']
        Map configuration = [flags: '-B']
        List configurationKeys = ['flags']
        Map merged = ConfigurationMerger.merge(parameters, parameterKeys, configuration, configurationKeys, defaults)
        assertEquals('mvn', merged.dockerImage)
        assertNull(merged.goals)
        assertEquals('', merged.flags)
    }

    @Test
    void testMergeParameterWithDefault(){
        Map defaults = [nonErpDestinations: []]
        Map parameters = [nonErpDestinations: null]
        List parameterKeys = ['nonErpDestinations']
        Map merged = ConfigurationMerger.merge(parameters, parameterKeys, defaults)
        assertEquals([], merged.nonErpDestinations)
    }
}