package com.salca.didaktikapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tvTop1: TextView
    private lateinit var tvTop2: TextView
    private lateinit var tvTop3: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        dbHelper = DatabaseHelper(this)

        val etName = findViewById<EditText>(R.id.etName)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        tvTop1 = findViewById(R.id.tvTop1)
        tvTop2 = findViewById(R.id.tvTop2)
        tvTop3 = findViewById(R.id.tvTop3)

        // ----------------------------------------------------
        // 1. CARGAR RANKING MUNDIAL
        // ----------------------------------------------------
        tvTop1.text = "Cargando..." // Feedback visual

        // Llamamos a nuestra función manual (sin Retrofit)
        SyncHelper.obtenerRankingMundial { rankingList ->
            // Volvemos al hilo principal para tocar la pantalla
            runOnUiThread {
                if (rankingList.isNotEmpty()) {
                    // Si hay internet y datos, mostramos el ranking nube
                    if (rankingList.isNotEmpty()) tvTop1.text = "🥇 1. ${rankingList[0]}"
                    else tvTop1.text = "🥇 1. -"

                    if (rankingList.size >= 2) tvTop2.text = "🥈 2. ${rankingList[1]}"
                    else tvTop2.text = "🥈 2. -"

                    if (rankingList.size >= 3) tvTop3.text = "🥉 3. ${rankingList[2]}"
                    else tvTop3.text = "🥉 3. -"
                } else {
                    // Si falló internet o está vacío, intentamos cargar el local
                    val local = dbHelper.getTop3Ranking()
                    if (local.isNotEmpty()) {
                        tvTop1.text = "🥇 1. ${local[0]}"
                        if (local.size >= 2) tvTop2.text = "🥈 2. ${local[1]}"
                        if (local.size >= 3) tvTop3.text = "🥉 3. ${local[2]}"
                    } else {
                        tvTop1.text = "⚠️ Offline / Sin datos"
                    }
                }
            }
        }
        // ----------------------------------------------------

        btnLogin.setOnClickListener {
            val studentName = etName.text.toString().trim()

            if (studentName.isNotEmpty()) {
                val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
                val editor = prefs.edit()
                editor.putString("nombre_alumno_actual", studentName)
                editor.apply()

                dbHelper.crearUsuarioInicial(studentName)

                // Feedback visual
                Toast.makeText(this, "Sartzen...", Toast.LENGTH_SHORT).show()
                btnLogin.isEnabled = false

                // Esperamos 1.5s antes de subir para asegurar que la BD está cerrada
                Handler(Looper.getMainLooper()).postDelayed({
                    SyncHelper.subirInmediatamente(this@LoginActivity)
                    val intent = Intent(this@LoginActivity, MapActivity::class.java)
                    startActivity(intent)
                    finish()
                }, 1500)

            } else {
                Toast.makeText(this, "Mesedez, idatzi zure izena", Toast.LENGTH_SHORT).show()
            }
        }
    }
}