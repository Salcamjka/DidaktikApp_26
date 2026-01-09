package com.salca.didaktikapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    // Declaramos la base de datos
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 1. Inicializamos la base de datos
        dbHelper = DatabaseHelper(this)

        val etName = findViewById<EditText>(R.id.etName)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val studentName = etName.text.toString().trim()

            if (studentName.isNotEmpty()) {

                // 2. GUARDAR EN BASE DE DATOS
                val guardado = dbHelper.addStudent(studentName)

                if (guardado) {
                    // Si se guardó bien, pasamos a la siguiente pantalla
                    val intent = Intent(this, MapActivity::class.java) // O MainActivity
                    intent.putExtra("STUDENT_NAME", studentName)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Errorea datu-basean", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(this, "Mesedez, idatzi zure izena", Toast.LENGTH_SHORT).show()
            }
        }
    }
}