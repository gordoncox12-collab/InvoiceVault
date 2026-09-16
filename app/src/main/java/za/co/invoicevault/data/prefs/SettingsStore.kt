package za.co.invoicevault.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import za.co.invoicevault.domain.AccentPalette
import za.co.invoicevault.domain.ThemeMode

private val Context.dataStore by preferencesDataStore("invoice_vault_settings")

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentPalette = AccentPalette.CAPE_GOLD,
    val defaultBusinessId: Long? = null
)

class SettingsStore(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val accentKey = stringPreferencesKey("accent")
    private val businessKey = longPreferencesKey("default_business_id")

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            themeMode = runCatching { ThemeMode.valueOf(prefs[themeKey] ?: ThemeMode.SYSTEM.name) }.getOrDefault(ThemeMode.SYSTEM),
            accent = runCatching { AccentPalette.valueOf(prefs[accentKey] ?: AccentPalette.CAPE_GOLD.name) }.getOrDefault(AccentPalette.CAPE_GOLD),
            defaultBusinessId = prefs[businessKey]
        )
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setAccent(palette: AccentPalette) {
        context.dataStore.edit { it[accentKey] = palette.name }
    }

    suspend fun setDefaultBusiness(id: Long?) {
        context.dataStore.edit { prefs ->
            if (id == null) prefs.remove(businessKey) else prefs[businessKey] = id
        }
    }
}
