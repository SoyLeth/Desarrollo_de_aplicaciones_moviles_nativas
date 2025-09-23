package com.example.tarea1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class ResumenFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_resumen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvNombre = view.findViewById<TextView>(R.id.tvNombre)
        val tvApellidos = view.findViewById<TextView>(R.id.tvApellidos)
        val tvFecha = view.findViewById<TextView>(R.id.tvFecha)
        val tvDireccion = view.findViewById<TextView>(R.id.tvDireccion)
        val tvGenero = view.findViewById<TextView>(R.id.tvGenero)
        val tvEstadoCivil = view.findViewById<TextView>(R.id.tvEstadoCivil)
        val tvOcupaciones = view.findViewById<TextView>(R.id.tvOcupaciones)
        val tvIntereses = view.findViewById<TextView>(R.id.tvIntereses)
        val ivFoto = view.findViewById<ImageView>(R.id.ivFotoResumen)

        val btnRegresar = view.findViewById<Button>(R.id.btnRegresarResumen)
        val btnFinalizar = view.findViewById<Button>(R.id.btnFinalizarResumen)
        val btnNuevoFormulario = view.findViewById<Button>(R.id.btnNuevoFormulario)

        // Mostrar los datos almacenados en DatosFormulario
        tvNombre.text = "Nombre: ${DatosFormulario.nombre ?: ""}"
        tvApellidos.text = "Apellidos: ${DatosFormulario.apellidos ?: ""}"
        tvFecha.text = "Fecha de nacimiento: ${DatosFormulario.fechaNacimiento ?: ""}"
        tvDireccion.text = "Dirección: ${DatosFormulario.direccion ?: ""}"
        tvGenero.text = "Género: ${DatosFormulario.genero ?: ""}"
        tvEstadoCivil.text = "Estado civil: ${DatosFormulario.estadoCivil ?: ""}"
        tvOcupaciones.text = "Ocupaciones: ${DatosFormulario.ocupaciones.joinToString(", ")}"
        tvIntereses.text = "Intereses: ${DatosFormulario.intereses.joinToString(", ")}"
        DatosFormulario.foto?.let { ivFoto.setImageBitmap(it) }

        // Botón regresar al fragmento anterior
        btnRegresar.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Finalizar y mostrar toast + mostrar botón de nuevo formulario
        btnFinalizar.setOnClickListener {
            Toast.makeText(requireContext(), "¡Información guardada correctamente!", Toast.LENGTH_LONG).show()
            btnNuevoFormulario.visibility = View.VISIBLE
        }

        // Nuevo formulario → limpiar datos y regresar al inicio
        btnNuevoFormulario.setOnClickListener {
            // Limpiar datos
            DatosFormulario.nombre = ""
            DatosFormulario.apellidos = ""
            DatosFormulario.fechaNacimiento = ""
            DatosFormulario.direccion = ""
            DatosFormulario.genero = ""
            DatosFormulario.estadoCivil = ""
            DatosFormulario.ocupaciones.clear()
            DatosFormulario.intereses.clear()
            DatosFormulario.foto = null

            // Volver al primer fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TextFieldsFragment())
                .commit()

            // Ocultar nuevamente el botón
            btnNuevoFormulario.visibility = View.GONE
        }
    }
}
