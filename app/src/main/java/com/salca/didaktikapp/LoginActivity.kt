package com.salca.didaktikapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        dbHelper = DatabaseHelper(this)

        val etName = findViewById<EditText>(R.id.etName)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val studentName = etName.text.toString().trim()

            if (studentName.isNotEmpty()) {
                // 1. Guardar nombre
                val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
                val editor = prefs.edit()
                editor.putString("nombre_alumno_actual", studentName)
                editor.apply()

                // 2. Crear usuario en BD local
                dbHelper.crearUsuarioInicial(studentName)

                // 3. ⚡ SINCRONIZAR AUTOMÁTICAMENTE AL ENTRAR ⚡
                // (Sin botón, lo hacemos invisible al usuario)
                SyncHelper.subirInmediatamente(this)

                // 4. Ir al Mapa
                val intent = Intent(this, MapActivity::class.java)
                startActivity(intent)
                finish()

            } else {
                Toast.makeText(this, "Mesedez, idatzi zure izena", Toast.LENGTH_SHORT).show()
            }
        }
    }
}