package com.example.tarea1

object DatosFormulario {
    var nombre: String? = null
    var apellidos: String? = null
    var fechaNacimiento: String? = null
    var direccion: String? = null

    var genero: String? = null
    var estadoCivil: String? = null
    var ocupaciones = mutableSetOf<String>() // ocupaciones del SeleccionFragment
    var viveConOtraPersona: Boolean = false

    var intereses = mutableSetOf<String>()  // intereses del ListasFragment
    var foto: android.graphics.Bitmap? = null
}
