package com.salca.didaktikapp

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

                // CORRECTO: Ahora addStudent solo pide el nombre
                val guardado = dbHelper.addStudent(studentName)

                if (guardado) {
                    val intent = Intent(this, MapActivity::class.java)
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