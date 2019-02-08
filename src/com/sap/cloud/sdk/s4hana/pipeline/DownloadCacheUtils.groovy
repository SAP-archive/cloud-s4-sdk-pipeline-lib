package com.sap.cloud.sdk.s4hana.pipeline

import com.sap.piper.DefaultValueCache

class DownloadCacheUtils implements Serializable {
    static final long serialVersionUID = 1L

    static String networkName() {
        return System.getenv('DL_CACHE_NETWORK')
    }

    static String hostname() {
        return System.getenv('DL_CACHE_HOSTNAME') ?: 's4sdk-nexus'
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
        String network = DefaultValueCache.getInstance().getDefaultValues().dockerNetwork
        if (network) {
            dockerOptions.add("--network=${network}")
        }
    }
}
