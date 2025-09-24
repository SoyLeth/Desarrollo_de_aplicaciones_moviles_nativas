package com.example.practica1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.practica1.R

class Fragment3 : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment3, container, false)

        val check = view.findViewById<CheckBox>(R.id.chkOption)
        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroup)
        val switch = view.findViewById<Switch>(R.id.switchOption)
        val txtResult = view.findViewById<TextView>(R.id.txtResultSelection)

        check.setOnCheckedChangeListener { _, isChecked ->
            txtResult.text = if (isChecked) "CheckBox activado ✅" else "CheckBox desmarcado ❌"
        }

        radioGroup.setOnCheckedChangeListener { _, id ->
            val radio = view.findViewById<RadioButton>(id)
            txtResult.text = "Seleccionaste: ${radio.text}"
        }

        switch.setOnCheckedChangeListener { _, isChecked ->
            txtResult.text = if (isChecked) "Switch ON 🔛" else "Switch OFF 🔴"
        }

        return view
    }
}
