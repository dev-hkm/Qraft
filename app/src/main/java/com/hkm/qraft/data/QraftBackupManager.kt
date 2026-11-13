package com.hkm.qraft.data

import com.google.gson.GsonBuilder
import com.hkm.qraft.model.QrCreationItem
import com.hkm.qraft.model.SavedStylePreset
import com.hkm.qraft.model.sanitized

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
            @Suppress("UNCHECKED_CAST")
            val rawItems = (parsed?.items as? List<QrCreationItem>)
            @Suppress("UNCHECKED_CAST")
            val rawPresets = (parsed?.customPresets as? List<SavedStylePreset>)
            if (parsed != null && (!rawItems.isNullOrEmpty() || !rawPresets.isNullOrEmpty() || parsed.formatVersion >= 1)) {
                parsed.copy(
                    items = rawItems.orEmpty().filterNotNull().map { it.sanitized() },
                    customPresets = rawPresets.orEmpty().filterNotNull().map { it.sanitized() }
                )
            } else {
                null
            }
        }.getOrNull()
    }
}
