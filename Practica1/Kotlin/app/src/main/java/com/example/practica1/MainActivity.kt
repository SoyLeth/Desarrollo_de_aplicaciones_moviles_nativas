package com.example.practica1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.practica1.fragments.*
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity(), Fragment2.OnButtonClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val btnToSecond = findViewById<Button>(R.id.btnToSecond)

        // Cargar fragment inicial
        loadFragment(Fragment1())

        // BottomNavigation para cambiar fragments
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_textfields -> loadFragment(Fragment1())
                R.id.nav_buttons -> loadFragment(Fragment2())
                R.id.nav_selection -> loadFragment(Fragment3())
                R.id.nav_list -> loadFragment(Fragment4())
                R.id.nav_info -> loadFragment(Fragment5())
            }
            true
        }

        // Botón para ir a SecondActivity
        btnToSecond.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }
    }

    // Función para cargar fragments en el contenedor
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // --- Interfaz de Fragment2 para mostrar Toasts ---
    override fun onNormalButtonClick() {
        Toast.makeText(this, "Pulsaste el botón normal 🚀", Toast.LENGTH_SHORT).show()
    }

    override fun onImageButtonClick() {
        Toast.makeText(this, "Pulsaste el botón con imagen 🖼️", Toast.LENGTH_SHORT).show()
    }
}
