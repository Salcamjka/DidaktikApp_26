package com.salca.didaktikapp

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class AccesibilidadActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accesibilidad)

        val switchContraste = findViewById<Switch>(R.id.switchContraste)
        val switchTexto = findViewById<Switch>(R.id.switchTexto)
        val btnVolver = findViewById<Button>(R.id.btnVolver)

        val sharedPref = getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        switchContraste.isChecked = sharedPref.getBoolean("MODO_OSCURO", false)
        switchTexto.isChecked = sharedPref.getBoolean("MODO_TEXTO_GRANDE", false)

        switchContraste.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("MODO_OSCURO", isChecked)
            editor.apply()
        }

        switchTexto.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("MODO_TEXTO_GRANDE", isChecked)
            editor.apply()
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }
}