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

/**
 * Pantalla de inicio de sesión y bienvenida de la aplicación.
 *
 * Sus funciones principales son:
 * * **Identificación:** Permite al alumno introducir su nombre para guardar su progreso.
 * * **Ranking Mundial:** Conecta con el servidor para descargar y mostrar el Top 3 de mejores puntuaciones globales.
 * * **Modo Offline:** Si no hay internet, muestra el ranking local almacenado en el dispositivo.
 * * **Gestión de Sesión:** Reinicia el estado de las actividades (juegos) para que el alumno pueda volver a jugar desde cero.
 *
 * @property dbHelper Referencia a la base de datos local para consultas de respaldo.
 * @author Nizam
 * @version 1.0
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    // Referencias a los textos del podio (Oro, Plata, Bronce)
    private lateinit var tvTop1: TextView
    private lateinit var tvTop2: TextView
    private lateinit var tvTop3: TextView

    /**
     * Método de inicio de la actividad.
     *
     * Carga el ranking asíncronamente y configura el botón de entrada.
     *
     * @param savedInstanceState Estado guardado de la instancia.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializamos la conexión con la Base de Datos Local
        dbHelper = DatabaseHelper(this)

        // --- 1. Referencias a la Interfaz ---
        val etName = findViewById<EditText>(R.id.etName)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        tvTop1 = findViewById(R.id.tvTop1)
        tvTop2 = findViewById(R.id.tvTop2)
        tvTop3 = findViewById(R.id.tvTop3)

        // --- 2. Carga del Ranking (Lógica Híbrida) ---
        tvTop1.text = "Kargatzen..." // Texto temporal mientras carga

        // Llamamos al servidor (API) para pedir el ranking mundial
        SyncHelper.obtenerRankingMundial { rankingList ->
            // Volvemos al hilo principal (UI Thread) para actualizar la pantalla
            runOnUiThread {
                if (rankingList.isNotEmpty()) {
                    // CASO A: Tenemos internet y el servidor responde
                    if (rankingList.isNotEmpty()) tvTop1.text = "🥇 1. ${rankingList[0]}"
                    else tvTop1.text = "🥇 1. -"

                    if (rankingList.size >= 2) tvTop2.text = "🥈 2. ${rankingList[1]}"
                    else tvTop2.text = "🥈 2. -"

                    if (rankingList.size >= 3) tvTop3.text = "🥉 3. ${rankingList[2]}"
                    else tvTop3.text = "🥉 3. -"
                } else {
                    // CASO B: Fallo de red o servidor vacío -> Usamos Ranking Local
                    val local = dbHelper.getTop3Ranking()
                    if (local.isNotEmpty()) {
                        tvTop1.text = "🥇 1. ${local[0]}"
                        if (local.size >= 2) tvTop2.text = "🥈 2. ${local[1]}"
                        if (local.size >= 3) tvTop3.text = "🥉 3. ${local[2]}"
                    } else {
                        // CASO C: Ni internet ni datos locales (App recién instalada)
                        tvTop1.text = "⚠️ Offline / Daturik gabe"
                    }
                }
            }
        }

        // --- 3. Lógica del Botón "Entrar" ---
        btnLogin.setOnClickListener {
            val studentName = etName.text.toString().trim()

            if (studentName.isNotEmpty()) {
                // Preparamos las Preferencias para guardar datos de sesión
                val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
                val editor = prefs.edit()

                // Guardamos el nombre del alumno actual para usarlo en otras pantallas
                editor.putString("nombre_alumno_actual", studentName)

                // ================================================================
                // 🔄 RESETEAMOS EL PROGRESO AL ENTRAR DE NUEVO
                // Esto asegura que si el alumno entra con el mismo nombre,
                // los marcadores del mapa vuelvan a ser ROJOS y pueda jugar de nuevo.
                // ================================================================
                editor.putBoolean("completado_ahorcado_$studentName", false)
                editor.putBoolean("completado_sopa_$studentName", false)
                editor.putBoolean("completado_txakurra_$studentName", false)
                editor.putBoolean("completado_puzzle_$studentName", false)
                editor.putBoolean("completado_muralla_$studentName", false)

                editor.apply() // Confirmamos los cambios en memoria

                // Registramos al usuario en la BD si es nuevo
                dbHelper.crearUsuarioInicial(studentName)

                // Desactivamos el botón para evitar que pulse dos veces rápido
                btnLogin.isEnabled = false

                // Esperamos 1.5 segundos para dar tiempo a guardar y subir datos iniciales
                Handler(Looper.getMainLooper()).postDelayed({
                    // Forzamos una subida de datos para asegurar que el usuario existe en la nube
                    SyncHelper.subirInmediatamente(this@LoginActivity)

                    // Navegamos al Mapa
                    val intent = Intent(this@LoginActivity, MapActivity::class.java)
                    startActivity(intent)
                    finish() // Cerramos el Login para que no pueda volver atrás
                }, 1500)

            } else {
                // Validación: El campo nombre no puede estar vacío
                Toast.makeText(this, "Mesedez, idatzi zure izena", Toast.LENGTH_SHORT).show()
            }
        }
    }
}