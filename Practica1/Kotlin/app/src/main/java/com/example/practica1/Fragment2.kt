package com.example.practica1

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.example.practica1.R

class Fragment2 : Fragment() {

    interface OnButtonClickListener {
        fun onNormalButtonClick()
        fun onImageButtonClick()
    }

    private var listener: OnButtonClickListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnButtonClickListener) listener = context
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment2, container, false)
        view.findViewById<Button>(R.id.btnNormal).setOnClickListener { listener?.onNormalButtonClick() }
        view.findViewById<ImageButton>(R.id.btnImage).setOnClickListener { listener?.onImageButtonClick() }
        return view
    }
}
