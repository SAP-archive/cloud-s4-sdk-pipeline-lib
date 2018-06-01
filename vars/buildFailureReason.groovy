class buildFailureReason implements Serializable {
    static String FAILURE_REASON

    final setFailureReason = { def failureReason ->
        if (FAILURE_REASON) {
            throw new IllegalStateException("Failure reason already exists and cannot be overwritten!")
        }
        buildFailureReason.FAILURE_REASON = failureReason
    }
}
