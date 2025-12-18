package com.example.actividad4.ui

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

private fun requiredBtPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION // necesario para discovery en ≤ Android 11
        )
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScreen(
    onBack: () -> Unit,
    onDevicePicked: (BluetoothDevice) -> Unit
) {
    val context = LocalContext.current
    val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    // Estado UI
    var hasAllPerms by remember { mutableStateOf(false) }
    var discoveryRunning by remember { mutableStateOf(false) }
    val devices = remember { mutableStateListOf<BluetoothDevice>() }
    var showRationale by remember { mutableStateOf(false) }
    var showGoSettings by remember { mutableStateOf(false) }

    // Launcher: pedir permisos en tiempo de ejecución
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val ok = requiredBtPermissions().all { perm -> grants[perm] == true }
        hasAllPerms = ok
        if (!ok) {
            // Usuario negó: si marcó "No volver a preguntar", sugiere ir a ajustes
            showGoSettings = true
        }
    }

    // Launcher: encender BT si está apagado
    val enableBtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Al volver del intent, intentamos iniciar discovery si procede
        if (adapter?.isEnabled == true && hasAllPerms) {
            startDiscovery(context, devices) { discoveryRunning = it }
        }
    }

    // BroadcastReceiver para ACTION_FOUND y ACTION_DISCOVERY_FINISHED
    DisposableEffect(Unit) {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        if (device != null && devices.none { it.address == device.address }) {
                            devices.add(device)
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        discoveryRunning = false
                    }
                }
            }
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) { /* ignorar */ }
            // Detener discovery si estaba corriendo
            adapter?.takeIf { it.isDiscovering }?.cancelDiscovery()
        }
    }

    // Checar permisos al entrar
    LaunchedEffect(Unit) {
        val allGranted = requiredBtPermissions().all { perm ->
            ContextCompat.checkSelfPermission(context, perm) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        hasAllPerms = allGranted
        if (!allGranted) {
            permissionLauncher.launch(requiredBtPermissions())
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Bluetooth") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Bluetooth, contentDescription = null)
                Text(
                    text = when {
                        adapter == null -> "Este dispositivo no tiene Bluetooth"
                        !hasAllPerms -> "Otorga permisos de Bluetooth"
                        !adapter.isEnabled -> "Bluetooth apagado"
                        else -> "Listo para buscar"
                    }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = adapter != null,
                    onClick = {
                        if (!hasAllPerms) {
                            permissionLauncher.launch(requiredBtPermissions())
                            return@Button
                        }
                        if (adapter?.isEnabled != true) {
                            // Intent nativo para habilitar BT
                            enableBtLauncher.launch(
                                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            )
                            return@Button
                        }
                        startDiscovery(context, devices) { discoveryRunning = it }
                    }
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (discoveryRunning) "Buscando..." else "Buscar usuarios")
                }

                OutlinedButton(
                    enabled = discoveryRunning,
                    onClick = {
                        stopDiscovery(context)
                        discoveryRunning = false
                    }
                ) { Text("Detener") }
            }

            Divider()

            Text("Dispositivos encontrados:")
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(devices) { d ->
                    DeviceRow(d) {
                        // Cancelar discovery antes de intentar conectar
                        stopDiscovery(context)
                        onDevicePicked(d)
                    }
                }
            }
        }
    }

    // Diálogo: ir a ajustes si el sistema bloqueó permisos
    if (showGoSettings) {
        AlertDialog(
            onDismissRequest = { showGoSettings = false },
            title = { Text("Permisos necesarios") },
            text = {
                Text(
                    "Para buscar y conectar por Bluetooth, concede los permisos en Ajustes:\n" +
                            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                "• Bluetooth Nearby (SCAN/CONNECT)\n"
                            else
                                "• Bluetooth y Ubicación (necesaria para discovery en Android 10/11)\n")
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showGoSettings = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text("Abrir Ajustes") }
            },
            dismissButton = {
                TextButton(onClick = { showGoSettings = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun DeviceRow(device: BluetoothDevice, onPick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPick() }
            .padding(vertical = 8.dp)
    ) {
        Text(text = device.name ?: "(sin nombre)")
        Text(text = device.address, style = MaterialTheme.typography.bodySmall)
    }
}

/** Inicia discovery de Bluetooth clásico, reiniciando lista. */
private fun startDiscovery(
    context: Context,
    sink: MutableList<BluetoothDevice>,
    setRunning: (Boolean) -> Unit
) {
    val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
    // limpiar lista y asegurar no haya un discovery previo
    sink.clear()
    if (adapter.isDiscovering) adapter.cancelDiscovery()
    setRunning(true)
    adapter.startDiscovery()
}

/** Detiene discovery si está corriendo. */
private fun stopDiscovery(context: Context) {
    val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
    if (adapter.isDiscovering) adapter.cancelDiscovery()
}
