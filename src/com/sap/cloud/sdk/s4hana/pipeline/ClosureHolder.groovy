package com.sap.cloud.sdk.s4hana.pipeline

/**
 * Keeps a {@link groovy.lang.Closure} and executes it on demand.
 */
final class ClosureHolder {
    final Closure closure

    ClosureHolder(Closure closure) {
        this.closure = closure
    }

    def execute() {
        this.closure()
    }
}
