package com.example.practica1.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.practica1.R

class Fragment5 : Fragment() {
    private var progress = 0
    private lateinit var progressBar: ProgressBar
    private lateinit var txtProgress: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment5, container, false)
        progressBar = view.findViewById(R.id.progressBar)
        txtProgress = view.findViewById(R.id.txtProgress)

        view.findViewById<ImageView>(R.id.imageView).setImageResource(android.R.drawable.ic_menu_gallery)
        view.findViewById<Button>(R.id.btnLoad).setOnClickListener {
            progress = 0
            progressBar.progress = 0
            val handler = Handler(Looper.getMainLooper())
            Thread {
                while (progress < 100) {
                    progress += 10
                    Thread.sleep(300)
                    handler.post {
                        progressBar.progress = progress
                        txtProgress.text = "Progreso: $progress%"
                    }
                }
            }.start()
        }
        return view
    }
}
