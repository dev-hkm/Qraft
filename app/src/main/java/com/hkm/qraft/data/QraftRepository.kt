package com.hkm.qraft.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.hkm.qraft.model.ExportImageFormat
import com.hkm.qraft.model.QrCreationItem
import com.hkm.qraft.model.SavedStylePreset
import com.hkm.qraft.model.ThemeMode
import com.hkm.qraft.model.sanitized
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.qraftDataStore by preferencesDataStore(name = "qraft_prefs")

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoVerifyScannability: Boolean = true,
    val defaultExportFormat: ExportImageFormat = ExportImageFormat.PNG,
    val defaultExportResolution: Int = 4096,
    val creatorName: String = "Creator"
)

class QraftRepository(private val context: Context) {

    private val gson: Gson = GsonBuilder().create()

    private object Keys {
        val ITEMS_JSON = stringPreferencesKey("saved_qr_items_json")
        val PRESETS_JSON = stringPreferencesKey("custom_presets_json")
        val SEEDED = booleanPreferencesKey("starter_seeded_v1")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AUTO_VERIFY = booleanPreferencesKey("auto_verify_scannability")
        val EXPORT_FORMAT = stringPreferencesKey("default_export_format")
        val EXPORT_RESOLUTION = intPreferencesKey("default_export_resolution")
        val CREATOR_NAME = stringPreferencesKey("creator_name")
    }

    val itemsFlow: Flow<List<QrCreationItem>> = context.qraftDataStore.data.map { prefs ->
        val seeded = prefs[Keys.SEEDED] ?: false
        val json = prefs[Keys.ITEMS_JSON]
        if (!seeded && json.isNullOrBlank()) {
            PresetCatalog.starterItems()
        } else if (json.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching {
                val type = object : TypeToken<List<QrCreationItem>>() {}.type
                gson.fromJson<List<QrCreationItem>>(json, type)?.filterNotNull()?.map { it.sanitized() } ?: emptyList()
            }.getOrElse { emptyList() }
        }
    }

    val customPresetsFlow: Flow<List<SavedStylePreset>> = context.qraftDataStore.data.map { prefs ->
        val json = prefs[Keys.PRESETS_JSON]
        if (json.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching {
                val type = object : TypeToken<List<SavedStylePreset>>() {}.type
                gson.fromJson<List<SavedStylePreset>>(json, type)?.filterNotNull()?.map { it.sanitized() } ?: emptyList()
            }.getOrElse { emptyList() }
        }
    }

    val preferencesFlow: Flow<UserPreferences> = context.qraftDataStore.data.map { prefs ->
        UserPreferences(
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            autoVerifyScannability = prefs[Keys.AUTO_VERIFY] ?: true,
            defaultExportFormat = runCatching {
                ExportImageFormat.valueOf(prefs[Keys.EXPORT_FORMAT] ?: ExportImageFormat.PNG.name)
            }.getOrDefault(ExportImageFormat.PNG),
            defaultExportResolution = prefs[Keys.EXPORT_RESOLUTION] ?: 4096,
            creatorName = prefs[Keys.CREATOR_NAME]?.takeIf { it.isNotBlank() } ?: "Creator"
        )
    }

    suspend fun ensureSeeded() {
        context.qraftDataStore.edit { prefs ->
            if (prefs[Keys.SEEDED] != true) {
                prefs[Keys.SEEDED] = true
                if (prefs[Keys.ITEMS_JSON].isNullOrBlank()) {
                    prefs[Keys.ITEMS_JSON] = gson.toJson(PresetCatalog.starterItems())
                }
            }
        }
    }

    suspend fun saveItem(item: QrCreationItem) {
        saveItems(listOf(item))
    }

    suspend fun saveItems(items: List<QrCreationItem>) {
        if (items.isEmpty()) return
        val now = System.currentTimeMillis()
        context.qraftDataStore.edit { prefs ->
            val current = decodeItems(prefs[Keys.ITEMS_JSON], prefs[Keys.SEEDED] == true).toMutableList()
            items.asReversed().forEach { item ->
                val index = current.indexOfFirst { it.id == item.id }
                val stamped = item.copy(updatedAtMillis = now)
                if (index >= 0) {
                    current[index] = stamped
                } else {
                    current.add(0, stamped)
                }
            }
            prefs[Keys.SEEDED] = true
            prefs[Keys.ITEMS_JSON] = gson.toJson(current)
        }
    }

    private suspend fun mutateItems(transform: (List<QrCreationItem>) -> List<QrCreationItem>) {
        context.qraftDataStore.edit { prefs ->
            val current = decodeItems(prefs[Keys.ITEMS_JSON], prefs[Keys.SEEDED] == true)
            prefs[Keys.SEEDED] = true
            prefs[Keys.ITEMS_JSON] = gson.toJson(transform(current))
        }
    }

    suspend fun toggleFavorite(id: String) {
        mutateItems { items ->
            items.map { if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it }
        }
    }

    suspend fun toggleArchived(id: String) {
        val now = System.currentTimeMillis()
        mutateItems { items ->
            items.map { if (it.id == id) it.copy(isArchived = !it.isArchived, updatedAtMillis = now) else it }
        }
    }

    suspend fun setArchivedForItems(ids: Set<String>, archived: Boolean) {
        if (ids.isEmpty()) return
        val now = System.currentTimeMillis()
        mutateItems { items ->
            items.map { if (it.id in ids) it.copy(isArchived = archived, updatedAtMillis = now) else it }
        }
    }

    suspend fun renameItem(id: String, newTitle: String) {
        val cleaned = newTitle.trim()
        if (cleaned.isBlank()) return
        val now = System.currentTimeMillis()
        mutateItems { items ->
            items.map { if (it.id == id) it.copy(title = cleaned, updatedAtMillis = now) else it }
        }
    }

    suspend fun updateItemOrganization(id: String, folder: String?, tags: List<String>) {
        val now = System.currentTimeMillis()
        val cleanFolder = folder?.trim()?.takeIf { it.isNotEmpty() }
        val cleanTags = tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        mutateItems { items ->
            items.map {
                if (it.id == id) it.copy(
                    folder = cleanFolder,
                    tags = cleanTags.takeIf { list -> list.isNotEmpty() },
                    updatedAtMillis = now
                ) else it
            }
        }
    }

    suspend fun deleteItem(id: String) {
        deleteItems(setOf(id))
    }

    suspend fun deleteItems(ids: Set<String>) {
        if (ids.isEmpty()) return
        mutateItems { items -> items.filterNot { it.id in ids } }
    }

    suspend fun setFavoriteForItems(ids: Set<String>, favorite: Boolean) {
        if (ids.isEmpty()) return
        mutateItems { items ->
            items.map { if (it.id in ids) it.copy(isFavorite = favorite) else it }
        }
    }

    suspend fun saveCustomPreset(preset: SavedStylePreset) {
        saveCustomPresets(listOf(preset))
    }

    suspend fun saveCustomPresets(presets: List<SavedStylePreset>) {
        if (presets.isEmpty()) return
        context.qraftDataStore.edit { prefs ->
            val current = decodePresets(prefs[Keys.PRESETS_JSON]).toMutableList()
            presets.asReversed().forEach { preset ->
                current.removeAll { it.id == preset.id }
                current.add(0, preset.copy(isBuiltIn = false))
            }
            prefs[Keys.PRESETS_JSON] = gson.toJson(current)
        }
    }

    suspend fun deleteCustomPreset(id: String) {
        context.qraftDataStore.edit { prefs ->
            val current = decodePresets(prefs[Keys.PRESETS_JSON]).filterNot { it.id == id }
            prefs[Keys.PRESETS_JSON] = gson.toJson(current)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.qraftDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAutoVerify(enabled: Boolean) {
        context.qraftDataStore.edit { it[Keys.AUTO_VERIFY] = enabled }
    }

    suspend fun setDefaultExportFormat(format: ExportImageFormat) {
        context.qraftDataStore.edit { it[Keys.EXPORT_FORMAT] = format.name }
    }

    suspend fun setDefaultExportResolution(resolution: Int) {
        context.qraftDataStore.edit { it[Keys.EXPORT_RESOLUTION] = resolution }
    }

    suspend fun setCreatorName(name: String) {
        context.qraftDataStore.edit { it[Keys.CREATOR_NAME] = name.trim().ifBlank { "Creator" } }
    }

    private fun decodeItems(json: String?, seeded: Boolean): List<QrCreationItem> {
        if (!seeded && json.isNullOrBlank()) return PresetCatalog.starterItems()
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val type = object : TypeToken<List<QrCreationItem>>() {}.type
            gson.fromJson<List<QrCreationItem>>(json, type)?.filterNotNull()?.map { it.sanitized() } ?: emptyList()
        }.getOrElse { emptyList() }
    }

    private fun decodePresets(json: String?): List<SavedStylePreset> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val type = object : TypeToken<List<SavedStylePreset>>() {}.type
            gson.fromJson<List<SavedStylePreset>>(json, type)?.filterNotNull()?.map { it.sanitized() } ?: emptyList()
        }.getOrElse { emptyList() }
    }
}
