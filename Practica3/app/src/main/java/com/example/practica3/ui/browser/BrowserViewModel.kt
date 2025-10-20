package com.example.practica3.ui.browser

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.practica3.data.FileItem
import com.example.practica3.data.FileRepository
import com.example.practica3.data.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BrowserViewModel(app: Application): AndroidViewModel(app) {
    private val repo = FileRepository(app)
    private val settings = SettingsDataStore(app)

    private val _rootUri = MutableStateFlow<Uri?>(null)
    val rootUri: StateFlow<Uri?> = _rootUri.asStateFlow()

    private val _currentDir = MutableStateFlow<Uri?>(null)
    val currentDir: StateFlow<Uri?> = _currentDir.asStateFlow()

    private val _items = MutableStateFlow<List<FileItem>>(emptyList())
    val items: StateFlow<List<FileItem>> = _items.asStateFlow()

    private val _isGrid = MutableStateFlow(false)
    val isGrid: StateFlow<Boolean> = _isGrid.asStateFlow()

    /** Pila de directorios para regresar. */
    private val dirStack = ArrayDeque<Uri>()

    /** --- Favoritos (persistentes) --- */
    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    /** --- Búsqueda --- */
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Lista visible = items filtrados por búsqueda. */
    val visibleItems: StateFlow<List<FileItem>> =
        combine(items, query) { list, q ->
            val qq = q.trim().lowercase()
            if (qq.isBlank()) list
            else list.filter { fi ->
                val name = fi.name.lowercase()
                val ext = (fi.extension ?: "").lowercase()
                name.contains(qq) || ext.contains(qq)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Favoritos del directorio actual (con búsqueda). */
    val favoriteItems: StateFlow<List<FileItem>> =
        combine(items, favorites, query) { list, favs, q ->
            val qq = q.trim().lowercase()
            list.filter { favs.contains(it.uri.toString()) }
                .let { favList ->
                    if (qq.isBlank()) favList
                    else favList.filter { fi ->
                        val name = fi.name.lowercase()
                        val ext = (fi.extension ?: "").lowercase()
                        name.contains(qq) || ext.contains(qq)
                    }
                }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Modelo ligero para recientes. */
    data class RecentEntry(
        val uri: Uri,
        val name: String,
        val isDirectory: Boolean
    )

    /** Recientes globales (pueden estar fuera del dir actual). */
    val recentItems: StateFlow<List<RecentEntry>> =
        settings.recentsFlow
            .map { list ->
                val ctx = getApplication<Application>()
                list.mapNotNull { s ->
                    runCatching {
                        val u = Uri.parse(s)
                        val doc = DocumentFile.fromSingleUri(ctx, u) ?: return@mapNotNull null
                        RecentEntry(
                            uri = u,
                            name = doc.name ?: u.lastPathSegment ?: "(sin nombre)",
                            isDirectory = doc.isDirectory
                        )
                    }.getOrNull()
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            val savedRoot = settings.rootUriFlow.first()
            _rootUri.value = savedRoot
            if (savedRoot != null) {
                dirStack.clear()
                dirStack.addLast(savedRoot)
            }
            _currentDir.value = savedRoot

            // cargar favoritos persistidos
            _favorites.value = settings.favoritesFlow.first()

            refresh()
        }
    }

    fun setRoot(uri: Uri, flags: Int, takePermission: (Uri, Int) -> Unit) {
        viewModelScope.launch {
            takePermission(uri, flags)
            settings.setRootUri(uri)
            _rootUri.value = uri
            dirStack.clear()
            dirStack.addLast(uri)
            _currentDir.value = uri
            refresh()
        }
    }

    fun openDir(uri: Uri) {
        if (_currentDir.value == uri) return
        dirStack.addLast(uri)
        _currentDir.value = uri
        viewModelScope.launch { refresh() }
    }

    fun goUp() {
        if (dirStack.size <= 1) return
        dirStack.removeLast()
        _currentDir.value = dirStack.last()
        viewModelScope.launch { refresh() }
    }

    fun toggleLayout() { _isGrid.value = !_isGrid.value }

    private suspend fun refresh() {
        val dir = _currentDir.value ?: run { _items.value = emptyList(); return }
        _items.value = repo.listChildren(dir)
    }

    // ========= Búsqueda =========
    fun setQuery(q: String) { _query.value = q }

    // ========= Favoritos =========
    fun isFavorite(uri: Uri): Boolean = _favorites.value.contains(uri.toString())

    fun toggleFavorite(uri: Uri) {
        val now = _favorites.value.toMutableSet()
        if (!now.add(uri.toString())) now.remove(uri.toString())
        _favorites.value = now
        // persistir
        viewModelScope.launch { settings.setFavorites(now) }
    }

    // ======================
    //  Acciones de gestión
    // ======================

    private fun currentDirDoc(): DocumentFile? {
        val ctx = getApplication<Application>()
        val dirUri = currentDir.value ?: return null
        return DocumentFile.fromTreeUri(ctx, dirUri)
    }

    suspend fun createFolder(name: String): Boolean = withContext(Dispatchers.IO) {
        val parent = currentDirDoc() ?: return@withContext false
        if (name.isBlank()) return@withContext false
        val exists = parent.findFile(name) != null
        if (exists) return@withContext false
        val ok = parent.createDirectory(name) != null
        if (ok) refresh()
        ok
    }

    suspend fun renameItem(itemUri: Uri, newName: String): Boolean = withContext(Dispatchers.IO) {
        val ctx = getApplication<Application>()
        val doc = DocumentFile.fromSingleUri(ctx, itemUri) ?: return@withContext false
        if (newName.isBlank()) return@withContext false
        val ok = doc.renameTo(newName)
        if (ok) refresh()
        ok
    }

    suspend fun deleteItem(itemUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val ctx = getApplication<Application>()
        val doc = DocumentFile.fromSingleUri(ctx, itemUri) ?: return@withContext false
        val ok = doc.delete()
        if (ok) refresh()
        ok
    }

    // ---- Copiar / Mover (base lista) ----
    private suspend fun copyStream(cr: ContentResolver, src: Uri, dst: Uri): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                cr.openInputStream(src).use { input ->
                    cr.openOutputStream(dst, "rwt").use { output ->
                        if (input == null || output == null) return@use
                        val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buf)
                            if (read <= 0) break
                            output.write(buf, 0, read)
                        }
                        output.flush()
                    }
                }
            }.isSuccess
        }

    suspend fun copyItem(itemUri: Uri, destDirUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val ctx = getApplication<Application>()
        val cr = ctx.contentResolver
        val src = DocumentFile.fromSingleUri(ctx, itemUri) ?: return@withContext false
        val destDir = DocumentFile.fromTreeUri(ctx, destDirUri) ?: return@withContext false
        val mime = if (src.isDirectory) "vnd.android.document/directory" else (src.type ?: "application/octet-stream")

        val result = if (src.isDirectory) {
            val newDir = destDir.createDirectory(src.name ?: "Carpeta") ?: return@withContext false
            src.listFiles().all { child ->
                if (child.isDirectory) {
                    runCatching {
                        val sub = newDir.createDirectory(child.name ?: "Carpeta") ?: return@all false
                        child.listFiles().all { subChild ->
                            if (subChild.isDirectory) {
                                copyItem(subChild.uri, sub.uri)
                            } else {
                                val newFile = sub.findFile(subChild.name ?: "")
                                    ?: sub.createFile(subChild.type ?: "application/octet-stream", subChild.name ?: "archivo")
                                newFile != null && copyStream(cr, subChild.uri, newFile!!.uri)
                            }
                        }
                    }.getOrDefault(false)
                } else {
                    val newFile = destDir.findFile(child.name ?: "")
                        ?: destDir.createFile(child.type ?: "application/octet-stream", child.name ?: "archivo")
                    newFile != null && copyStream(cr, child.uri, newFile!!.uri)
                }
            }
        } else {
            val target = destDir.findFile(src.name ?: "")
                ?: destDir.createFile(mime, src.name ?: "archivo")
            target != null && copyStream(cr, src.uri, target!!.uri)
        }

        if (result) refresh()
        result
    }

    suspend fun moveItem(itemUri: Uri, destDirUri: Uri): Boolean {
        val ok = copyItem(itemUri, destDirUri)
        if (ok) deleteItem(itemUri)
        return ok
    }

    // ========= Recientes =========
    fun trackOpened(uri: Uri) {
        viewModelScope.launch { settings.appendRecent(uri.toString()) }
    }

    fun clearRecents() {
        viewModelScope.launch { settings.clearRecents() }
    }
}
