package com.example.practica1

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.practica1.fragments.*

class SecondActivity : AppCompatActivity(), Fragment2.OnButtonClickListener, Fragment1.OnTextSubmitListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Activity 2"

        supportFragmentManager.beginTransaction()
            .replace(R.id.containerTop, Fragment1())
            .commit()
        supportFragmentManager.beginTransaction()
            .replace(R.id.containerMiddle, Fragment2())
            .commit()
        supportFragmentManager.beginTransaction()
            .replace(R.id.containerBottom, Fragment5())
            .commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onNormalButtonClick() {
        Toast.makeText(this, "Pulsaste el botón normal 🚀", Toast.LENGTH_SHORT).show()
    }

    override fun onImageButtonClick() {
        Toast.makeText(this, "Pulsaste el botón con imagen 🖼️", Toast.LENGTH_SHORT).show()
    }

    override fun onTextSubmitted(text: String) {
        Toast.makeText(this, "Texto recibido: $text ✏️", Toast.LENGTH_SHORT).show()
    }
}
