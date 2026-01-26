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

        // Referencias
        val switchContraste = findViewById<Switch>(R.id.switchContraste)
        val switchTexto = findViewById<Switch>(R.id.switchTexto)
        val btnVolver = findViewById<Button>(R.id.btnVolver)

        // 1. ABRIR MEMORIA
        val sharedPref = getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        // 2. LEER ESTADO ANTERIOR
        switchContraste.isChecked = sharedPref.getBoolean("MODO_OSCURO", false)
        switchTexto.isChecked = sharedPref.getBoolean("MODO_TEXTO_GRANDE", false)

        // 3. GUARDAR CAMBIOS: ALTO CONTRASTE
        switchContraste.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("MODO_OSCURO", isChecked)
            editor.apply()
        }

        // 4. GUARDAR CAMBIOS: TEXTO GRANDE
        switchTexto.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("MODO_TEXTO_GRANDE", isChecked)
            editor.apply()
        }

        // EL CÓDIGO DE VOZ SE HA ELIMINADO

        btnVolver.setOnClickListener {
            finish()
        }
    }
}