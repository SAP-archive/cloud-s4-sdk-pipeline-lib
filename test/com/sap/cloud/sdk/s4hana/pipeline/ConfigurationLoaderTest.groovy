package com.sap.cloud.sdk.s4hana.pipeline

import org.junit.Test

import static org.junit.Assert.assertEquals

class ConfigurationLoaderTest {

    private static getScript() {
        Map configuration = [:]
        configuration.general = [productiveBranch: 'master']
        configuration.steps = [executeMaven: [dockerImage: 'maven:3.2-jdk-8-onbuild']]
        configuration.stages = [staticCodeChecks: [pmdExcludes: '**']]

        Map defaultConfiguration = [:]
        defaultConfiguration.general = [productiveBranch: 'develop']
        defaultConfiguration.steps = [executeGradle: [dockerImage: 'gradle:4.0.1-jdk8']]
        defaultConfiguration.stages = [staticCodeChecks: [pmdExcludes: '*.java']]

        def pipelineEnvironment = [configuration: configuration, defaultConfiguration: defaultConfiguration]
        return [pipelineEnvironment: pipelineEnvironment]
    }

    @Test
    void testLoadStepConfiguration() {
        Map config = ConfigurationLoader.stepConfiguration(getScript(), 'executeMaven')
        assertEquals('maven:3.2-jdk-8-onbuild', config.dockerImage)
    }

    @Test
    void testLoadStageConfiguration() {
        Map config = ConfigurationLoader.stageConfiguration(getScript(), 'staticCodeChecks')
        assertEquals('**', config.pmdExcludes)
    }

    @Test
    void testLoadGeneralConfiguration() {
        Map config = ConfigurationLoader.generalConfiguration(getScript())
        assertEquals('master', config.productiveBranch)
    }

    @Test
    void testLoadDefaultStepConfiguration() {
        Map config = ConfigurationLoader.defaultStepConfiguration(getScript(), 'executeGradle')
        assertEquals('gradle:4.0.1-jdk8', config.dockerImage)
    }

    @Test
    void testLoadDefaultStageConfiguration() {
        Map config = ConfigurationLoader.defaultStageConfiguration(getScript(), 'staticCodeChecks')
        assertEquals('*.java', config.pmdExcludes)
    }

    @Test
    void testLoadDefaultGeneralConfiguration() {
        Map config = ConfigurationLoader.defaultGeneralConfiguration(getScript())
        assertEquals('develop', config.productiveBranch)
    }
}