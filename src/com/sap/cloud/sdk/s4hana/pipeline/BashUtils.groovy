package com.sap.cloud.sdk.s4hana.pipeline

class BashUtils implements Serializable {
    static final long serialVersionUID = 1L

    static String escape(String str) {
        return "\"${str.replace("\"", "\\\"")}\""
    }
}
