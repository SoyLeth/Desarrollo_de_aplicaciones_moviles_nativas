@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.example.practica3.ui.browser

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.practica3.data.FileItem
import com.example.practica3.ui.theme.BrandTheme
import com.example.practica3.ui.theme.LocalBrandTheme   // <-- NUEVO
import kotlinx.coroutines.launch

// Enum a nivel de archivo (evita crash del compilador FIR)
enum class BrowserTab { ARCHIVOS, FAVORITOS, RECIENTES }

@Composable
fun BrowserScreen(
    vm: BrowserViewModel,
    onPickRoot: () -> Unit,
    onChangeTheme: (BrandTheme) -> Unit
) {
    val context = LocalContext.current
    val rootUri by vm.rootUri.collectAsStateWithLifecycle()
    val currentDir by vm.currentDir.collectAsStateWithLifecycle()
    val items by vm.visibleItems.collectAsStateWithLifecycle()
    val favItems by vm.favoriteItems.collectAsStateWithLifecycle()
    val recents by vm.recentItems.collectAsStateWithLifecycle()
    val isGrid by vm.isGrid.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()

    val currentBrand = LocalBrandTheme.current        // <-- NUEVO

    var themeMenu by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<FileItem?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    var currentTab by remember { mutableStateOf(BrowserTab.ARCHIVOS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestor de Archivos") },
                navigationIcon = {
                    IconButton(
                        onClick = { vm.goUp() },
                        enabled = currentDir != null && currentDir != rootUri && currentTab != BrowserTab.RECIENTES
                    ) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Arriba") }
                },
                actions = {
                    if (currentTab != BrowserTab.RECIENTES) {
                        IconButton(onClick = { vm.toggleLayout() }) {
                            Icon(if (isGrid) Icons.Outlined.List else Icons.Outlined.GridView, null)
                        }
                    }
                    Box {
                        IconButton(onClick = { themeMenu = true }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Tema y raíz")
                        }
                        DropdownMenu(expanded = themeMenu, onDismissRequest = { themeMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Tema Guinda (IPN)") },
                                trailingIcon = {
                                    if (currentBrand == BrandTheme.GUINDA) Icon(Icons.Outlined.Check, contentDescription = null)
                                },
                                onClick = { themeMenu = false; onChangeTheme(BrandTheme.GUINDA) }
                            )
                            DropdownMenuItem(
                                text = { Text("Tema Azul (ESCOM)") },
                                trailingIcon = {
                                    if (currentBrand == BrandTheme.AZUL) Icon(Icons.Outlined.Check, contentDescription = null)
                                },
                                onClick = { themeMenu = false; onChangeTheme(BrandTheme.AZUL) }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text(if (rootUri == null) "Elegir carpeta raíz" else "Cambiar carpeta raíz") },
                                onClick = { themeMenu = false; onPickRoot() }
                            )
                            if (currentTab == BrowserTab.RECIENTES) {
                                Divider()
                                DropdownMenuItem(
                                    text = { Text("Limpiar recientes") },
                                    onClick = {
                                        themeMenu = false
                                        vm.clearRecents()
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentTab == BrowserTab.ARCHIVOS) {
                ExtendedFloatingActionButton(onClick = {
                    newName = ""
                    showCreate = true
                }) { Text("Nueva carpeta") }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            TabRow(selectedTabIndex = currentTab.ordinal) {
                Tab(
                    selected = currentTab == BrowserTab.ARCHIVOS,
                    onClick = { currentTab = BrowserTab.ARCHIVOS },
                    text = { Text("Archivos") },
                    icon = { Icon(Icons.Outlined.Folder, null) }
                )
                Tab(
                    selected = currentTab == BrowserTab.FAVORITOS,
                    onClick = { currentTab = BrowserTab.FAVORITOS },
                    text = { Text("Favoritos") },
                    icon = { Icon(Icons.Outlined.Star, null) }
                )
                Tab(
                    selected = currentTab == BrowserTab.RECIENTES,
                    onClick = { currentTab = BrowserTab.RECIENTES },
                    text = { Text("Recientes") },
                    icon = { Icon(Icons.Outlined.History, null) }
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = { vm.setQuery(it) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                placeholder = { Text("Buscar por nombre o extensión…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Limpiar")
                        }
                    }
                }
            )

            when (currentTab) {
                BrowserTab.ARCHIVOS -> {
                    Breadcrumbs(currentDir)
                    if (rootUri == null) {
                        EmptyState(onPickRoot)
                    } else {
                        if (items.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Sin resultados")
                            }
                        } else {
                            if (isGrid)
                                GridView(
                                    items = items,
                                    isFav = { vm.isFavorite(it.uri) },
                                    onToggleFav = { vm.toggleFavorite(it.uri) },
                                    onOpen = { openItem(context, it, vm) },
                                    onOpenExternal = { openExternal(context, it.uri) },
                                    onLong = { item ->
                                        selected = item; newName = item.name; showSheet = true
                                    }
                                )
                            else
                                ListView(
                                    items = items,
                                    isFav = { vm.isFavorite(it.uri) },
                                    onToggleFav = { vm.toggleFavorite(it.uri) },
                                    onOpen = { openItem(context, it, vm) },
                                    onOpenExternal = { openExternal(context, it.uri) },
                                    onLong = { item ->
                                        selected = item; newName = item.name; showSheet = true
                                    }
                                )
                        }
                    }
                }

                BrowserTab.FAVORITOS -> {
                    if (favItems.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Sin favoritos en esta carpeta")
                        }
                    } else {
                        if (isGrid)
                            GridView(
                                items = favItems,
                                isFav = { vm.isFavorite(it.uri) },
                                onToggleFav = { vm.toggleFavorite(it.uri) },
                                onOpen = { openItem(context, it, vm) },
                                onOpenExternal = { openExternal(context, it.uri) },
                                onLong = { item ->
                                    selected = item; newName = item.name; showSheet = true
                                }
                            )
                        else
                            ListView(
                                items = favItems,
                                isFav = { vm.isFavorite(it.uri) },
                                onToggleFav = { vm.toggleFavorite(it.uri) },
                                onOpen = { openItem(context, it, vm) },
                                onOpenExternal = { openExternal(context, it.uri) },
                                onLong = { item ->
                                    selected = item; newName = item.name; showSheet = true
                                }
                            )
                    }
                }

                BrowserTab.RECIENTES -> {
                    val filtered = recents.filter {
                        val q = query.trim().lowercase()
                        q.isBlank() || it.name.lowercase().contains(q)
                    }
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No hay recientes")
                        }
                    } else {
                        LazyColumn {
                            items(filtered.size, key = { filtered[it].uri.toString() }) { idx ->
                                val r = filtered[idx]
                                ListItem(
                                    headlineContent = { Text(r.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    supportingContent = {
                                        Text(if (r.isDirectory) "Carpeta" else "Archivo", maxLines = 1)
                                    },
                                    leadingContent = {
                                        if (r.isDirectory) Icon(Icons.Outlined.Folder, null)
                                        else Icon(Icons.Outlined.Description, null)
                                    },
                                    trailingContent = {
                                        if (!r.isDirectory) IconButton(onClick = {
                                            openExternal(context, r.uri)
                                        }) { Icon(Icons.Outlined.OpenInNew, null) }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                if (r.isDirectory) vm.openDir(r.uri)
                                                else {
                                                    vm.trackOpened(r.uri)
                                                    openExternal(context, r.uri)
                                                }
                                            },
                                            onLongClick = { /* opcional: quitar de recientes */ }
                                        )
                                )
                                Divider()
                            }
                        }
                    }
                }
            }
        }
    }

    // Hoja de acciones (renombrar / eliminar)
    if (showSheet && selected != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(selected!!.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    showSheet = false
                    showRename = true
                }) { Text("Renombrar") }
                Spacer(Modifier.height(8.dp))
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        showSheet = false
                        showDelete = true
                    }
                ) { Text("Eliminar") }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // Diálogos (crear / renombrar / eliminar)
    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Crear carpeta") },
            text = {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("Nombre") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCreate = false
                    scope.launch { vm.createFolder(newName.trim()) }
                }) { Text("Crear") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancelar") } }
        )
    }

    if (showRename && selected != null) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Renombrar") },
            text = {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("Nuevo nombre") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = selected
                    showRename = false
                    if (target != null) scope.launch { vm.renameItem(target.uri, newName.trim()) }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancelar") } }
        )
    }

    if (showDelete && selected != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Eliminar") },
            text = { Text("¿Seguro que deseas eliminar \"${selected!!.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    val target = selected
                    showDelete = false
                    if (target != null) scope.launch { vm.deleteItem(target.uri) }
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun FileThumb(item: FileItem) {
    val isImage = (item.extension ?: "").lowercase() in
            setOf("png", "jpg", "jpeg", "webp", "gif", "svg")

    if (!isImage) {
        Icon(Icons.Outlined.Description, contentDescription = null)
        return
    }

    val ctx = LocalContext.current
    val req = remember(item.uri) {
        ImageRequest.Builder(ctx)
            .data(item.uri)
            .size(Size.ORIGINAL)
            .allowHardware(false)
            .crossfade(true)
            .build()
    }
    AsyncImage(
        model = req,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(6.dp))
    )
}

@Composable
private fun ListView(
    items: List<FileItem>,
    isFav: (FileItem) -> Boolean,
    onToggleFav: (FileItem) -> Unit,
    onOpen: (FileItem) -> Unit,
    onOpenExternal: (FileItem) -> Unit,
    onLong: (FileItem) -> Unit
) {
    LazyColumn {
        items(items.size, key = { items[it].uri.toString() }) { idx ->
            val item = items[idx]
            ListItem(
                headlineContent = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = {
                    Text(
                        "${if (item.isDirectory) "Carpeta" else item.extension ?: "Archivo"}  ·  ${item.formattedSize()}  ·  ${item.formattedDate()}",
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                },
                leadingContent = {
                    if (item.isDirectory) Icon(Icons.Outlined.Folder, contentDescription = null)
                    else FileThumb(item)
                },
                trailingContent = {
                    Row {
                        if (!item.isDirectory) {
                            IconButton(onClick = { onOpenExternal(item) }) {
                                Icon(Icons.Outlined.OpenInNew, contentDescription = "Abrir con otra app")
                            }
                        }
                        IconButton(onClick = { onToggleFav(item) }) {
                            Icon(
                                if (isFav(item)) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorito"
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onOpen(item) },
                        onLongClick = { onLong(item) }
                    )
            )
            Divider()
        }
    }
}

@Composable
private fun GridView(
    items: List<FileItem>,
    isFav: (FileItem) -> Boolean,
    onToggleFav: (FileItem) -> Unit,
    onOpen: (FileItem) -> Unit,
    onOpenExternal: (FileItem) -> Unit,
    onLong: (FileItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(items.size, key = { items[it].uri.toString() }) { idx ->
            val item = items[idx]
            ElevatedCard(
                modifier = Modifier
                    .padding(6.dp)
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onOpen(item) },
                        onLongClick = { onLong(item) }
                    )
            ) {
                Column(Modifier.padding(12.dp)) {
                    if (item.isDirectory) Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(32.dp))
                    else FileThumb(item)
                    Spacer(Modifier.height(8.dp))
                    Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(item.formattedSize(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onToggleFav(item) }) {
                            Icon(
                                if (isFav(item)) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorito"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Breadcrumbs(current: Uri?) {
    Surface(tonalElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = current?.lastPathSegment ?: "—",
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EmptyState(onPickRoot: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(8.dp))
            Text("Selecciona una carpeta raíz para empezar")
            Spacer(Modifier.height(16.dp))
            Button(onClick = onPickRoot) { Text("Elegir carpeta raíz") }
        }
    }
}

/** Abre carpeta o, si es archivo, lanza "Abrir con…" y trackea reciente. */
private fun openItem(
    context: android.content.Context,
    item: FileItem,
    vm: BrowserViewModel
) {
    if (item.isDirectory) {
        vm.openDir(item.uri)
    } else {
        vm.trackOpened(item.uri)
        openExternal(context, item.uri)
    }
}

private fun openExternal(context: android.content.Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
}
