package com.sap.cloud.sdk.s4hana.pipeline

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

class ManifestUpdaterTest {
    private Map additionaEnvValues = [
            destinations: 'newValue'
    ]

    @Test
    void testSetNewEnvironmentVariables() {
        Map manifest = [
                applications: [
                        [
                                name: 'appname',
                                host: 'someHost'
                        ]
                ]
        ]
        ManifestUpdater updater = new ManifestUpdater(manifest)
        updater.addEnvironmentsVariables(additionaEnvValues)

        assertNotNull(updater.getManifest().applications[0].env)
        assertEquals('newValue', updater.getManifest().applications[0].env.destinations)
    }

    @Test
    void testAddEnvironmentVariables() {
        Map manifest = [
                applications: [
                        [
                                name: 'appname',
                                host: 'someHost',
                                env : [
                                        TARGET_RUNTIME: 'main'
                                ]
                        ]
                ]
        ]

        ManifestUpdater updater = new ManifestUpdater(manifest)
        updater.addEnvironmentsVariables(additionaEnvValues)

        assertEquals('newValue', updater.getManifest().applications[0].env.destinations)
    }

    @Test
    void testDoNotReplaceEnvironmentVariables() {
        Map manifest = [
                applications: [
                        [
                                name: 'appname',
                                host: 'someHost',
                                env : [
                                        TARGET_RUNTIME: 'main',
                                        destinations  : 'someDestination'
                                ]
                        ]
                ]
        ]

        ManifestUpdater updater = new ManifestUpdater(manifest)
        updater.addEnvironmentsVariables(additionaEnvValues)

        assertEquals('someDestination', updater.getManifest().applications[0].env.destinations)
    }
}
