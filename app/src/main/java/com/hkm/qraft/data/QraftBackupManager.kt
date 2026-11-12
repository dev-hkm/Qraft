package com.hkm.qraft.data

import com.google.gson.GsonBuilder
import com.hkm.qraft.model.QrCreationItem
import com.hkm.qraft.model.SavedStylePreset

data class QraftBackupBundle(
    val formatVersion: Int = 1,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val appVersion: String = "4.0.0",
    val items: List<QrCreationItem> = emptyList(),
    val customPresets: List<SavedStylePreset> = emptyList()
)

object QraftBackupManager {

    private val prettyGson = GsonBuilder().setPrettyPrinting().create()

    fun serialize(
        items: List<QrCreationItem>,
        customPresets: List<SavedStylePreset>,
        appVersion: String = "4.0.0"
    ): String {
        val bundle = QraftBackupBundle(
            formatVersion = 1,
            exportedAtMillis = System.currentTimeMillis(),
            appVersion = appVersion,
            items = items,
            customPresets = customPresets
        )
        return prettyGson.toJson(bundle)
    }

    fun deserialize(rawJson: String): QraftBackupBundle? {
        if (rawJson.isBlank()) return null
        return runCatching {
            val parsed = prettyGson.fromJson(rawJson, QraftBackupBundle::class.java)
            if (parsed != null && (parsed.items.isNotEmpty() || parsed.customPresets.isNotEmpty() || parsed.formatVersion >= 1)) {
                parsed.copy(
                    items = parsed.items.filterNotNull(),
                    customPresets = parsed.customPresets.filterNotNull()
                )
            } else {
                null
            }
        }.getOrNull()
    }
}
