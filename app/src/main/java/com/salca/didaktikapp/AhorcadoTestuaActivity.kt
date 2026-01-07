package com.dm2.ahorcadodidaktikapp

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AhorcadoTestuaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ahorcado_testua)

        // Configurar el botón de esta nueva pantalla
        val btnSiguiente = findViewById<Button>(R.id.btnJarraitu)
        // En AhorcadoTestuaActivity.kt
        btnSiguiente.setOnClickListener {
            val intent = android.content.Intent(this, AhorcadoAudioaActivity::class.java)
            startActivity(intent)
        }
    }
}