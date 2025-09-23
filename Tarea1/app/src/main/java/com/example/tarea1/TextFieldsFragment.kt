package com.example.tarea1

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.util.Calendar

class TextFieldsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_textfields, container, false)

        val etNombre = view.findViewById<EditText>(R.id.etNombre)
        val etApellidos = view.findViewById<EditText>(R.id.etApellidos)
        val etFechaNacimiento = view.findViewById<EditText>(R.id.etFechaNacimiento)
        val etDireccion = view.findViewById<EditText>(R.id.etDireccion)
        val btnSiguiente = view.findViewById<Button>(R.id.btnSiguiente)

        etFechaNacimiento.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(requireContext(),
                { _, year, month, day ->
                    etFechaNacimiento.setText(String.format("%02d/%02d/%04d", day, month + 1, year))
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            datePicker.show()
        }

        btnSiguiente.setOnClickListener {
            if (etNombre.text.isBlank() || etApellidos.text.isBlank() ||
                etFechaNacimiento.text.isBlank() || etDireccion.text.isBlank()
            ) {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
            } else {
                DatosFormulario.nombre = etNombre.text.toString()
                DatosFormulario.apellidos = etApellidos.text.toString()
                DatosFormulario.fechaNacimiento = etFechaNacimiento.text.toString()
                DatosFormulario.direccion = etDireccion.text.toString()

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SeleccionFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        return view
    }
}
