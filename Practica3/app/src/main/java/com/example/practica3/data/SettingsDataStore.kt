package com.example.practica3.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.practica3.ui.theme.BrandTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

object Keys {
    // Tema / raíz
    val BRAND = intPreferencesKey("brand")            // 0=GUINDA, 1=AZUL
    val ROOT_URI = stringPreferencesKey("root_uri")

    // Favoritos y recientes
    val FAVORITES = stringSetPreferencesKey("favorites")     // set de URIs (String)
    val RECENTS = stringPreferencesKey("recents_pipe")       // lista '|' separada
}

class SettingsDataStore(private val context: Context) {

    // ========= Tema / raíz =========
    val brandFlow: Flow<BrandTheme> = context.dataStore.data.map { p ->
        when (p[Keys.BRAND] ?: 0) { 1 -> BrandTheme.AZUL; else -> BrandTheme.GUINDA }
    }

    val rootUriFlow: Flow<Uri?> = context.dataStore.data.map { p ->
        p[Keys.ROOT_URI]?.let(Uri::parse)
    }

    suspend fun setBrand(brand: BrandTheme) {
        context.dataStore.edit { it[Keys.BRAND] = if (brand == BrandTheme.AZUL) 1 else 0 }
    }

    suspend fun setRootUri(uri: Uri?) {
        context.dataStore.edit { prefs ->
            if (uri == null) {
                prefs.remove(Keys.ROOT_URI)
            } else {
                prefs[Keys.ROOT_URI] = uri.toString()
            }
        }
    }

    // ========= Favoritos =========
    val favoritesFlow: Flow<Set<String>> = context.dataStore.data.map { p ->
        p[Keys.FAVORITES] ?: emptySet()
    }

    suspend fun setFavorites(set: Set<String>) {
        context.dataStore.edit { it[Keys.FAVORITES] = set }
    }

    // ========= Recientes =========
    // Guardamos una lista de URIs separadas por '|', sin duplicados, con capacidad máxima.
    val recentsFlow: Flow<List<String>> = context.dataStore.data.map { p ->
        p[Keys.RECENTS]
            ?.split('|')
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    suspend fun appendRecent(uriString: String, capacity: Int = 20) {
        context.dataStore.edit { prefs ->
            val raw = prefs[Keys.RECENTS].orEmpty()
            val list = raw.split('|').filter { it.isNotBlank() }.toMutableList()
            // mover al frente si ya existe
            list.remove(uriString)
            list.add(0, uriString)
            // recortar a capacidad
            while (list.size > capacity) list.removeLast()
            prefs[Keys.RECENTS] = list.joinToString("|")
        }
    }

    suspend fun clearRecents() {
        context.dataStore.edit { it.remove(Keys.RECENTS) }
    }
}
