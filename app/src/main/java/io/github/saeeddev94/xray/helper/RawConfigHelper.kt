package io.github.saeeddev94.xray.helper

import io.github.saeeddev94.xray.Settings
import io.github.saeeddev94.xray.database.Config
import java.io.File

class RawConfigHelper(
    private val settings: Settings,
    private val config: Config,
    private val baseConfig: String,
    private val rawConfigFile: File,
) {

    /**
     * Get the final config:
     * - If raw config is enabled and file exists, return raw config content
     * - Otherwise, use the generic pipeline to generate config from parts
     */
    fun getConfig(): String {
        return if (settings.rawConfigEnabled && rawConfigFile.exists()) {
            // Use raw config from file
            rawConfigFile.readText()
        } else {
            // Use generated config from parts (dns, inbounds, outbounds, routing, etc.)
            val configHelper = ConfigHelper(settings, config, baseConfig)
            configHelper.toString()
        }
    }

    /**
     * Check if raw config mode is active
     */
    fun isRawConfigActive(): Boolean {
        return settings.rawConfigEnabled && rawConfigFile.exists()
    }
}
