package com.sap.cloud.sdk.s4hana.pipeline

import org.junit.Test

import static org.junit.Assert.assertEquals

class BashUtilsTest {
    @Test
    void escapeFilePath() {
        // Given: A Windows-style file path C:\some\path
        def input = 'C:\\some\\path'

        // When we escape the string
        def result = BashUtils.escape(input)

        // Then the string is surrounded by single quotes 'C:\some\path'
        def expected = "'C:\\some\\path'"
        assertEquals(expected, result)
    }

    @Test
    void escapeUri() {
        // Given: An URI with single quotes values http://www.sap.com?$filter='234'
        def input = "http://www.sap.com?\$filter='234'"

        // When we escape the string
        def result = BashUtils.escape(input)

        // Then the input string is surrounded by single quotes and each original ' is replaced by '"'"'
        // 'http://www.sap.com?$filter='"'"'234'"'"''
        def expected = "'http://www.sap.com?\$filter='\"'\"'234'\"'\"''"
        assertEquals(expected, result)
    }

    @Test
    void escapePassword() {
        // Given: A random generated password VQ5r\%*h"49'Ch>Jj?
        def input = "VQ5r\\%*h\"49'Ch>Jj?"

        // When we escape the string
        def result = BashUtils.escape(input)

        // Then the input string is surrounded by single quotes and each original ' is replaced by '"'"'
        // 'VQ5r\%*h"49'"'"'Ch>Jj?'
        def expected = "'VQ5r\\%*h\"49'\"'\"'Ch>Jj?'"
        assertEquals(expected, result)
    }
}
