package com.example.practica3

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.practica3.data.SettingsDataStore
import com.example.practica3.ui.browser.BrowserScreen
import com.example.practica3.ui.browser.BrowserViewModel
import com.example.practica3.ui.theme.BrandTheme
import com.example.practica3.ui.theme.GestorTheme
import com.example.practica3.ui.theme.LocalBrandTheme
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    private val vm: BrowserViewModel by viewModels()

    private val openTree = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            // Concedemos permiso persistente de lectura/escritura
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            vm.setRoot(
                uri = it,
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                takePermission = { _, _ -> /* ya lo tomamos arriba */ }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settings = SettingsDataStore(this)
        val initialBrand = runBlocking { settings.brandFlow.first() }

        setContent {
            // Estado local para cambiar el tema sin recrear la Activity
            val (brand, setBrand) = remember { mutableStateOf(initialBrand) }

            CompositionLocalProvider(LocalBrandTheme provides brand) {
                GestorTheme(brand = brand) {
                    BrowserScreen(
                        vm = vm,
                        onPickRoot = { openTree.launch(null) },
                        onChangeTheme = { newBrand: BrandTheme ->
                            // Persistimos y actualizamos el estado Compose (sin recreate)
                            runBlocking { settings.setBrand(newBrand) }
                            setBrand(newBrand)
                        }
                    )
                }
            }
        }
    }
}
