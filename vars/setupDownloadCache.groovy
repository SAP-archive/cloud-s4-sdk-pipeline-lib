import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils
import com.sap.piper.ConfigurationLoader
import com.sap.piper.DefaultValueCache

def call(Map parameters) {
    def script = parameters.script

    if (DownloadCacheUtils.isCacheActive()) {
        echo "Download cache for maven and npm activated"

        writeFile file: s4SdkGlobals.mavenGlobalSettingsFile, text: libraryResource("mvn_download_cache_proxy_settings.xml")

        // FIXME: Here we missuse the defaultConfiguration to control behavior in npm steps (will be merged with other values)
        DefaultValueCache.getInstance().getDefaultValues().dockerNetwork = DownloadCacheUtils.networkName()

        // FIXME For maven we use the default settings (possible because mavenExecute never sets any own dockerOptions)
        Map defaultMavenConfiguration = ConfigurationLoader.defaultStepConfiguration(script, 'mavenExecute')

        defaultMavenConfiguration.dockerOptions = DownloadCacheUtils.downloadCacheNetworkParam()
        defaultMavenConfiguration.globalSettingsFile = s4SdkGlobals.mavenGlobalSettingsFile

        Map npmDefaultConfiguration = ConfigurationLoader.defaultStepConfiguration(script, 'executeNpm')
        npmDefaultConfiguration.defaultNpmRegistry = "http://s4sdk-nexus:8081/repository/npm-proxy"

        if (ConfigurationLoader.stepConfiguration(script, 'executeNpm').defaultNpmRegistry) {
            println("[WARNING]: Pipeline configuration contains custom value for 'executeNpm.defaultNpmRegistry'. " +
                "The download cache will not be used for npm builds. To setup a npm-proxy, specify it in your 'server.cfg' file.")
        }

    } else {
        echo "Download cache for maven and npm not activated"
    }
}
