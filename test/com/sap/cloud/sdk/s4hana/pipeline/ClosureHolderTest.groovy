package com.sap.cloud.sdk.s4hana.pipeline

import org.junit.Test

import static org.junit.Assert.assertEquals

class ClosureHolderTest {
    @Test
    void 'ClosureHolder executes a Groovy Closure as expected'() {
        // given
        def holder = new ClosureHolder({return 'some value'})

        // when
        def result = holder.execute()

        // then
        assertEquals('some value', result)
    }
}
