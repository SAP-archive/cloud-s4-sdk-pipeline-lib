package com.sap.cloud.sdk.s4hana.pipeline

import org.junit.Test

import static org.junit.Assert.assertEquals

class BashUtilsTest {

    @Test
    void testEscape() {
        // input: 'a$b%c%d$e'$?$#$$"'
        def input = "\'a\$b%c%d\$e\'\$?\$#\$\$\"\'"
        // expect: "'a$b%c%d$e'$?$#$$\"'"
        def expected = "\"\'a\$b%c%d\$e\'\$?\$#\$\$\\\"\'\""
        assertEquals(expected, BashUtils.escape(input))
    }
}
