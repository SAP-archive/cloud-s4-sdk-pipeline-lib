package com.sap.cloud.sdk.s4hana.pipeline

class DownloadCacheUtils implements Serializable {
    static final long serialVersionUID = 1L

    static String networkName() {
        return System.getenv('DL_CACHE_NETWORK')
    }

    static boolean isCacheActive() {
        return networkName() != null
    }

    static String downloadCacheNetworkParam() {
        if (isCacheActive()) {
            return " --network=${networkName()}"
        } else {
            return ""
        }
    }

    static appendDownloadCacheNetworkOption(def script, List dockerOptions) {
        String network = script.pipelineEnvironment.defaultConfiguration.dockerNetwork
        if(network) {
            dockerOptions.add("--network=${network}")
        }
    }
}
