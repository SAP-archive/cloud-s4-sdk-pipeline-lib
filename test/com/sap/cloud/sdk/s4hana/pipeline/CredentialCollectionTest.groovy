package com.sap.cloud.sdk.s4hana.pipeline

import static groovy.test.GroovyAssert.*
import org.junit.Test

class CredentialCollectionTest {

    @Test
    void testToCredentialJson() {
        def credentials = new CredentialCollection()
        def credential1 = new Credential("alias1", "username1", "password1")
        def credential2 = new Credential("alias2", "username2", "password2")
        credentials.addCredential(credential1)
        credentials.addCredential(credential2)
        def expected = '{ "credentials": [\n  {"alias":"alias1","username":"username1","password":"password1"},\n  {"alias":"alias2","username":"username2","password":"password2"}\n]}\n'
        assertEquals(expected, credentials.toCredentialJson() as String)
    }
}