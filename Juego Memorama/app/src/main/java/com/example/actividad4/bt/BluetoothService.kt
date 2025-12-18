package com.example.actividad4.bt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.ParcelUuid
import java.io.IOException
import java.util.UUID
import kotlin.concurrent.thread

/**
 * Servicio Bluetooth clásico (RFCOMM) con:
 * - Anuncio/aceptación por UUID propio (SDP)
 * - Descubrimiento que filtra automáticamente dispositivos compatibles
 *   y muestra SOLO teléfonos (clase PHONE o nombre que sugiere móvil).
 * - Conexión insegura (sin emparejar) usando RFCOMM inseguro.
 */
class BluetoothService(
    private val context: Context,
    private val onDeviceFound: (BluetoothDevice) -> Unit,
    private val onDiscoveryFinished: () -> Unit,
    private val onConnected: (BluetoothSocket, fromServer: Boolean) -> Unit,
    private val onError: (Throwable) -> Unit
) {

    companion object {
        // ✅ UUID propio del juego (debe ser el mismo en host y cliente)
        val SERVICE_UUID: UUID = UUID.fromString("e6c26e85-2e6c-4f9b-9c6a-6f0b9a40a9c0")
        const val SERVICE_NAME: String = "MemoramaBt"
    }

    private val adapter: BluetoothAdapter? by lazy {
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mgr.adapter
    }

    private var serverThread: Thread? = null
    private var serverSocket: BluetoothServerSocket? = null
    private var discoveryRegistered = false

    // Dispositivos vistos durante discovery (evitamos duplicados)
    private val seenAddresses = mutableSetOf<String>()

    // Dispositivos pendientes de conocer UUIDs (fetchUuidsWithSdp)
    private val pendingUuidQuery = mutableSetOf<String>()

    /** Receiver de discovery + UUIDs */
    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val dev: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val clazz: BluetoothClass? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS)
                    if (dev != null && dev.address != null && seenAddresses.add(dev.address)) {
                        // 🔎 Solo celulares
                        if (!isLikelyPhone(dev, clazz)) return@onReceive

                        // Si está emparejado, intenta obtener UUIDs (ACTION_UUID)
                        if (dev.bondState == BluetoothDevice.BOND_BONDED) {
                            pendingUuidQuery.add(dev.address)
                            dev.fetchUuidsWithSdp()
                        } else {
                            // Muchos equipos no publican UUID sin pairing; aún así mostrar por ser teléfono
                            onDeviceFound(dev)
                        }
                    }
                }

                BluetoothDevice.ACTION_UUID -> {
                    val dev: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val parcelUuids =
                        intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)

                    if (dev != null) {
                        // Mantén solo celulares
                        if (!isLikelyPhone(dev, dev.bluetoothClass)) {
                            pendingUuidQuery.remove(dev.address)
                            return@onReceive
                        }

                        if (parcelUuids != null) {
                            val uuids = parcelUuids
                                .filterIsInstance<ParcelUuid>()
                                .map { it.uuid }
                                .toSet()
                            // Si expone nuestro UUID, genial; si no, igual lo mostramos por ser teléfono
                            if (SERVICE_UUID in uuids) {
                                onDeviceFound(dev)
                            } else {
                                onDeviceFound(dev)
                            }
                        } else {
                            // Sin UUIDs: igualmente mostrar por ser teléfono
                            onDeviceFound(dev)
                        }
                        pendingUuidQuery.remove(dev.address)
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    onDiscoveryFinished()
                }
            }
        }
    }

    /** Debe llamarse desde Composable (p.ej. en DisposableEffect) */
    fun register() {
        if (discoveryRegistered) return
        discoveryRegistered = true
        val f = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_UUID)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(discoveryReceiver, f)
    }

    fun unregister() {
        if (!discoveryRegistered) return
        discoveryRegistered = false
        try {
            context.unregisterReceiver(discoveryReceiver)
        } catch (_: Throwable) {
        }
    }

    fun isSupported(): Boolean = adapter != null
    fun isEnabled(): Boolean = adapter?.isEnabled == true

    fun requestEnable(ctx: Context) {
        // Delega al sistema (mostrar diálogo)
        ctx.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    /** Limpia el estado de discovery interno. */
    private fun resetDiscoveryState() {
        seenAddresses.clear()
        pendingUuidQuery.clear()
    }

    /** Inicia búsqueda de dispositivos (filtrado visual por "solo celulares"). */
    fun startDiscovery() {
        val ad = adapter ?: return
        resetDiscoveryState()
        if (ad.isDiscovering) ad.cancelDiscovery()
        ad.startDiscovery()
    }

    fun stopDiscovery() {
        val ad = adapter ?: return
        if (ad.isDiscovering) ad.cancelDiscovery()
    }

    /** Hilo servidor: publica SDP con nuestro UUID y acepta la primera conexión. */
    fun startServer() {
        val ad = adapter ?: return
        stopServer()

        serverThread = thread(name = "BtServer") {
            try {
                // ✅ RFCOMM inseguro (no requiere emparejar)
                serverSocket = ad.listenUsingInsecureRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
                val socket = serverSocket!!.accept() // Bloquea hasta que llegue un cliente
                onConnected(socket, /*fromServer=*/true)
            } catch (t: Throwable) {
                onError(t)
            } finally {
                try {
                    serverSocket?.close()
                } catch (_: IOException) {
                }
                serverSocket = null
            }
        }
    }

    fun stopServer() {
        try {
            serverSocket?.close()
        } catch (_: IOException) {
        }
        serverSocket = null
        serverThread?.interrupt()
        serverThread = null
    }

    /** Cliente: conecta a un dispositivo por nuestro UUID (inseguro, sin pairing). */
    fun connectTo(device: BluetoothDevice) {
        val ad = adapter ?: return
        thread(name = "BtClient") {
            var socket: BluetoothSocket? = null
            try {
                // Cancelamos discovery para acelerar conexión
                if (ad.isDiscovering) ad.cancelDiscovery()
                // ✅ RFCOMM inseguro (no requiere emparejar)
                socket = device.createInsecureRfcommSocketToServiceRecord(SERVICE_UUID)
                socket.connect()
                onConnected(socket, /*fromServer=*/false)
            } catch (t: Throwable) {
                try {
                    socket?.close()
                } catch (_: IOException) {
                }
                onError(t)
            }
        }
    }

    /** Nombre BT del dispositivo local. */
    fun selfName(): String = adapter?.name ?: "(desconocido)"

    fun requestDiscoverable(ctx: Context, seconds: Int = 180) {
        val i = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, seconds)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(i)
    }

    /**
     * Dirección BT local (en Android 12+ normalmente se oculta y devuelve 02:00:00:00:00:00
     * a apps sin privilegios). La mostramos solo informativa.
     */
    @SuppressLint("HardwareIds")
    fun selfAddress(): String = adapter?.address ?: "02:00:00:00:00:00"

    /** Devuelve true si el dispositivo es un teléfono o su nombre sugiere que es un móvil. */
    private fun isLikelyPhone(dev: BluetoothDevice, clazz: BluetoothClass?): Boolean {
        // 1) Filtro fuerte por clase mayor PHONE
        if (clazz?.majorDeviceClass == BluetoothClass.Device.Major.PHONE) return true
        // 2) Heurística por nombre (algunos equipos no reportan clase correcta)
        val n = dev.name?.lowercase()?.trim().orEmpty()
        if (n.isEmpty()) return false
        val hints = listOf(
            "phone", "iphone", "android", "xiaomi", "samsung", "huawei", "pixel",
            "redmi", "motorola", "moto", "oneplus", "oppo", "realme", "honor",
            "vivo", "nokia", "zte", "tecno", "infinix"
        )
        return hints.any { n.contains(it) }
    }
}
