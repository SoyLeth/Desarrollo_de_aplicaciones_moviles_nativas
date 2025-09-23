package com.example.tarea1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class ListasFragment : Fragment() {

    private val listaItems = listOf(
        "Deportes", "Música", "Lectura", "Viajes", "Cine", "Videojuegos", "Arte"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_listas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listView = view.findViewById<ListView>(R.id.listView)
        val btnRegresar = view.findViewById<Button>(R.id.btnRegresarListas)
        val btnSiguiente = view.findViewById<Button>(R.id.btnSiguienteListas)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_multiple_choice, listaItems)
        listView.adapter = adapter
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        // Restaurar selección desde DatosFormulario
        listaItems.forEachIndexed { index, item ->
            listView.setItemChecked(index, DatosFormulario.intereses.contains(item))
        }

        // Guardar selección en DatosFormulario
        listView.setOnItemClickListener { _, _, position, _ ->
            val item = listaItems[position]
            if (listView.isItemChecked(position)) DatosFormulario.intereses.add(item)
            else DatosFormulario.intereses.remove(item)
        }

        btnRegresar.setOnClickListener { parentFragmentManager.popBackStack() }

        btnSiguiente.setOnClickListener {
            if (DatosFormulario.intereses.isEmpty()) {
                Toast.makeText(requireContext(), "Selecciona al menos un interés", Toast.LENGTH_SHORT).show()
            } else {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, FotoFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
}
