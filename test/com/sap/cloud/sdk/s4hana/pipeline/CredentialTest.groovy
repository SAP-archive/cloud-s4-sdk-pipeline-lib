package com.sap.cloud.sdk.s4hana.pipeline

import org.junit.Test
import static org.junit.Assert.assertEquals

class CredentialTest {

    @Test
    void testToString() {
        def credential = new Credential("alias1", "username1", "password1")
        def expected = "{\"alias\":\"alias1\",\"username\":\"username1\",\"password\":\"password1\"}"
        assertEquals(expected, credential.toString())
    }
}