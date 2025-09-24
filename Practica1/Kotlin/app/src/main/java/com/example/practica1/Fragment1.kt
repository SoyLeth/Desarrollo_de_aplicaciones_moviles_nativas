package com.example.practica1

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.practica1.R

class Fragment1 : Fragment() {

    interface OnTextSubmitListener {
        fun onTextSubmitted(text: String)
    }

    private var listener: OnTextSubmitListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnTextSubmitListener) listener = context
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment1, container, false)

        val input = view.findViewById<EditText>(R.id.editTextName)
        val button = view.findViewById<Button>(R.id.btnShowText)
        val result = view.findViewById<TextView>(R.id.txtResult)

        button.setOnClickListener {
            val text = input.text.toString()
            result.text = "Hola, $text!"
            listener?.onTextSubmitted(text)
        }
        return view
    }
}
