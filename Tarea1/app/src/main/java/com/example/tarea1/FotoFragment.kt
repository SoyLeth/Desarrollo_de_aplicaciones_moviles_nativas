package com.example.tarea1

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment

class FotoFragment : Fragment() {

    companion object {
        private const val PICK_IMAGE = 1
    }

    private var fotoBitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_foto, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivFoto = view.findViewById<ImageView>(R.id.ivFoto)
        val btnCargar = view.findViewById<Button>(R.id.btnCargarFoto)
        val btnSiguiente = view.findViewById<Button>(R.id.btnSiguienteFoto)
        val btnRegresar = view.findViewById<Button>(R.id.btnRegresarFoto)
        val btnVolverCargar = view.findViewById<Button>(R.id.btnVolverCargar)

        // Inicialmente mostrar solo cargar y regresar
        btnSiguiente.visibility = View.GONE
        btnVolverCargar.visibility = View.GONE

        // Mostrar foto si ya había sido cargada
        DatosFormulario.foto?.let {
            ivFoto.setImageBitmap(it)
            fotoBitmap = it

            // Solo mostrar los botones que queremos
            btnCargar.visibility = View.GONE
            btnSiguiente.visibility = View.VISIBLE
            btnVolverCargar.visibility = View.VISIBLE
            btnRegresar.visibility = View.VISIBLE
        }

        // Botón cargar foto
        btnCargar.setOnClickListener {
            pickImage()
        }

        // Botón volver a cargar
        btnVolverCargar.setOnClickListener {
            pickImage()
        }

        // Botón siguiente → ir al resumen
        btnSiguiente.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ResumenFragment())
                .addToBackStack(null)
                .commit()
        }

        // Botón regresar al fragment anterior
        btnRegresar.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImage: Uri? = data.data
            selectedImage?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, it)
                fotoBitmap = bitmap
                DatosFormulario.foto = bitmap

                // Mostrar la imagen
                view?.findViewById<ImageView>(R.id.ivFoto)?.setImageBitmap(bitmap)

                // Solo mostrar los botones siguientes, volver a cargar y regresar
                view?.findViewById<Button>(R.id.btnCargarFoto)?.visibility = View.GONE
                view?.findViewById<Button>(R.id.btnSiguienteFoto)?.visibility = View.VISIBLE
                view?.findViewById<Button>(R.id.btnVolverCargar)?.visibility = View.VISIBLE
                view?.findViewById<Button>(R.id.btnRegresarFoto)?.visibility = View.VISIBLE
            }
        }
    }
}
