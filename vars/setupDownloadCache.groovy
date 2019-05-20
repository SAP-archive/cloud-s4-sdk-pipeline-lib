import com.sap.cloud.sdk.s4hana.pipeline.DownloadCacheUtils
import com.sap.piper.ConfigurationLoader
import com.sap.piper.DefaultValueCache

def call(Map parameters) {
    def script = parameters.script

    if (DownloadCacheUtils.isCacheActive()) {
        echo "Download cache for maven and npm activated"

        String mavenSettingsTemplate = libraryResource("mvn_download_cache_proxy_settings.xml")
        String hostname = DownloadCacheUtils.hostname()
        // trailing '/' is needed for whitesource scan
        String defaultNpmRegistry = "http://${hostname}:8081/repository/npm-proxy/"
        String mavenSettings = mavenSettingsTemplate.replace('__HOSTNAME__', hostname)
        writeFile file: s4SdkGlobals.mavenGlobalSettingsFile, text: mavenSettings

        // FIXME: Here we misuse the defaultConfiguration to control behavior in npm steps (will be merged with other values)
        DefaultValueCache.getInstance().getDefaultValues().dockerNetwork = DownloadCacheUtils.networkName()

        // FIXME For maven we use the default settings (possible because mavenExecute never sets any own dockerOptions)
        Map defaultMavenConfiguration = ConfigurationLoader.defaultStepConfiguration(script, 'mavenExecute')

        defaultMavenConfiguration.dockerOptions = DownloadCacheUtils.downloadCacheNetworkParam()
        defaultMavenConfiguration.globalSettingsFile = s4SdkGlobals.mavenGlobalSettingsFile

        Map defaultMtaBuildConfiguration = ConfigurationLoader.defaultStepConfiguration(script, 'mtaBuild')

        defaultMtaBuildConfiguration.dockerOptions = DownloadCacheUtils.downloadCacheNetworkParam()
        defaultMtaBuildConfiguration.globalSettingsFile = s4SdkGlobals.mavenGlobalSettingsFile
        defaultMtaBuildConfiguration.defaultNpmRegistry = defaultNpmRegistry

        Map npmDefaultConfiguration = ConfigurationLoader.defaultStepConfiguration(script, 'executeNpm')
        npmDefaultConfiguration.defaultNpmRegistry = defaultNpmRegistry

        if (ConfigurationLoader.stepConfiguration(script, 'executeNpm').defaultNpmRegistry) {
            println("[WARNING]: Pipeline configuration contains custom value for 'executeNpm.defaultNpmRegistry'. " +
                "The download cache will not be used for npm builds. To setup a npm-proxy, specify it in your 'server.cfg' file.")
        }

    } else {
        echo "Download cache for maven and npm not activated"
    }
}
