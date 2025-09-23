package com.example.tarea1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class SeleccionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_seleccion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rgGenero = view.findViewById<RadioGroup>(R.id.rgGenero)
        val rgEstadoCivil = view.findViewById<RadioGroup>(R.id.rgEstadoCivil)
        val cbEstudiante = view.findViewById<CheckBox>(R.id.cbEstudiante)
        val cbEmpleado = view.findViewById<CheckBox>(R.id.cbEmpleado)
        val cbDesempleado = view.findViewById<CheckBox>(R.id.cbDesempleado)
        val cbIndependiente = view.findViewById<CheckBox>(R.id.cbIndependiente)
        val swViveConPersona = view.findViewById<Switch>(R.id.swViveConPersona)
        val btnRegresar = view.findViewById<Button>(R.id.btnRegresarSeleccion)
        val btnSiguiente = view.findViewById<Button>(R.id.btnSiguienteSeleccion)

        // Restaurar selección
        when (DatosFormulario.genero) {
            "Masculino" -> rgGenero.check(R.id.rbMasculino)
            "Femenino" -> rgGenero.check(R.id.rbFemenino)
            "Otro" -> rgGenero.check(R.id.rbOtro)
        }
        when (DatosFormulario.estadoCivil) {
            "Soltero(a)" -> rgEstadoCivil.check(R.id.rbSoltero)
            "Casado(a)" -> rgEstadoCivil.check(R.id.rbCasado)
            "Unión libre" -> rgEstadoCivil.check(R.id.rbUnionLibre)
            "Otro" -> rgEstadoCivil.check(R.id.rbOtroEstadoCivil)
        }
        cbEstudiante.isChecked = DatosFormulario.ocupaciones.contains("Estudiante")
        cbEmpleado.isChecked = DatosFormulario.ocupaciones.contains("Empleado")
        cbDesempleado.isChecked = DatosFormulario.ocupaciones.contains("Desempleado")
        cbIndependiente.isChecked = DatosFormulario.ocupaciones.contains("Independiente")
        swViveConPersona.isChecked = DatosFormulario.viveConOtraPersona

        // Guardar selección al cambiar
        rgGenero.setOnCheckedChangeListener { _, checkedId ->
            DatosFormulario.genero = when (checkedId) {
                R.id.rbMasculino -> "Masculino"
                R.id.rbFemenino -> "Femenino"
                R.id.rbOtro -> "Otro"
                else -> null
            }
        }
        rgEstadoCivil.setOnCheckedChangeListener { _, checkedId ->
            DatosFormulario.estadoCivil = when (checkedId) {
                R.id.rbSoltero -> "Soltero(a)"
                R.id.rbCasado -> "Casado(a)"
                R.id.rbUnionLibre -> "Unión libre"
                R.id.rbOtroEstadoCivil -> "Otro"
                else -> null
            }
        }
        cbEstudiante.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) DatosFormulario.ocupaciones.add("Estudiante") else DatosFormulario.ocupaciones.remove("Estudiante")
        }
        cbEmpleado.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) DatosFormulario.ocupaciones.add("Empleado") else DatosFormulario.ocupaciones.remove("Empleado")
        }
        cbDesempleado.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) DatosFormulario.ocupaciones.add("Desempleado") else DatosFormulario.ocupaciones.remove("Desempleado")
        }
        cbIndependiente.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) DatosFormulario.ocupaciones.add("Independiente") else DatosFormulario.ocupaciones.remove("Independiente")
        }
        swViveConPersona.setOnCheckedChangeListener { _, isChecked ->
            DatosFormulario.viveConOtraPersona = isChecked
        }

        btnRegresar.setOnClickListener { parentFragmentManager.popBackStack() }

        btnSiguiente.setOnClickListener {
            // Validación: al menos género, estado civil y ocupaciones
            if (DatosFormulario.genero == null || DatosFormulario.estadoCivil == null || DatosFormulario.ocupaciones.isEmpty()) {
                Toast.makeText(requireContext(), "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            } else {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ListasFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
}
