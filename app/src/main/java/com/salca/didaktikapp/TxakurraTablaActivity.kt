package com.salca.didaktikapp

import android.content.Context
import android.content.Intent // <--- IMPORTANTE: Necesario para ir al Mapa
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.jvm.java

class TxakurraTablaActivity : AppCompatActivity() {

    private lateinit var btnFinish: Button
    private lateinit var ivMascota: ImageView
    private lateinit var ivPerro: ImageView
    private lateinit var ivLeon: ImageView

    // Campos de texto
    private lateinit var etLehoia1: EditText
    private lateinit var etLehoia2: EditText
    private lateinit var etLehoia3: EditText
    private lateinit var etLehoia4: EditText
    private lateinit var etLehoia5: EditText

    private lateinit var etTxakurra1: EditText
    private lateinit var etTxakurra2: EditText
    private lateinit var etTxakurra3: EditText
    private lateinit var etTxakurra4: EditText
    private lateinit var etTxakurra5: EditText

    // Lista para gestionar todos los campos de golpe
    private val allEditTexts = mutableListOf<EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_txakurra_tabla)

        initializeViews()
        setupTextWatchers() // Configurar la vigilancia del texto
        setupFinishButton()
        animateInitialElements()
    }

    private fun initializeViews() {
        btnFinish = findViewById(R.id.btnFinish)
        ivMascota = findViewById(R.id.ivMascota)
        ivPerro = findViewById(R.id.ivPerro)
        ivLeon = findViewById(R.id.ivLeon)

        etLehoia1 = findViewById(R.id.etLehoia1)
        etLehoia2 = findViewById(R.id.etLehoia2)
        etLehoia3 = findViewById(R.id.etLehoia3)
        etLehoia4 = findViewById(R.id.etLehoia4)
        etLehoia5 = findViewById(R.id.etLehoia5)

        etTxakurra1 = findViewById(R.id.etTxakurra1)
        etTxakurra2 = findViewById(R.id.etTxakurra2)
        etTxakurra3 = findViewById(R.id.etTxakurra3)
        etTxakurra4 = findViewById(R.id.etTxakurra4)
        etTxakurra5 = findViewById(R.id.etTxakurra5)

        // Metemos todos en la lista
        allEditTexts.addAll(listOf(
            etLehoia1, etLehoia2, etLehoia3, etLehoia4, etLehoia5,
            etTxakurra1, etTxakurra2, etTxakurra3, etTxakurra4, etTxakurra5
        ))
    }

    // --- VIGILAR SI ESCRIBEN ---
    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                validarTabla() // Comprobar estado cada vez que se escribe
            }
        }

        // Añadir el vigilante a cada campo
        allEditTexts.forEach { it.addTextChangedListener(watcher) }
    }

    // --- ACTIVAR / DESACTIVAR BOTÓN ---
    private fun validarTabla() {
        // Comprobar si TODOS tienen texto (quitando espacios en blanco)
        val estaCompleto = allEditTexts.all { it.text.toString().trim().isNotEmpty() }

        btnFinish.isEnabled = estaCompleto

        if (estaCompleto) {
            // ACTIVADO: Usamos el color txakurra (Rojo suave)
            val colorActivo = ContextCompat.getColor(this, R.color.txakurra)
            btnFinish.backgroundTintList = ColorStateList.valueOf(colorActivo)
        } else {
            // DESACTIVADO: Color Gris
            val colorDesactivado = ContextCompat.getColor(this, R.color.boton_desactivado)
            btnFinish.backgroundTintList = ColorStateList.valueOf(colorDesactivado)
        }
    }

    private fun animateInitialElements() {
        // Usamos try-catch por seguridad si no existen las animaciones
        try {
            val waveAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_wave)
            ivMascota.startAnimation(waveAnim)

            val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_bounce_in)
            ivPerro.startAnimation(bounceAnim)

            Handler(Looper.getMainLooper()).postDelayed({
                ivLeon.startAnimation(bounceAnim)
            }, 300)
        } catch (e: Exception) { }
    }

    private fun animateMascotaCelebracion() {
        try {
            ivMascota.setImageResource(R.drawable.leonfeliz)
            val celebrateAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_celebrate)
            ivMascota.startAnimation(celebrateAnim)
        } catch (e: Exception) { }
    }

    private fun setupFinishButton() {
        btnFinish.setOnClickListener {
            // Animación y guardado
            animateMascotaCelebracion()
            guardarPuntuacionEnBD(100)

            Toast.makeText(this, "🎉 Bikain! Taula osatu duzu! (+100 pts)", Toast.LENGTH_LONG).show()

            // Esperar 2 segundos y volver al MAPA
            Handler(Looper.getMainLooper()).postDelayed({

                // --- CAMBIO AQUÍ: IR AL MAPA ---
                val intent = Intent(this, MapActivity::class.java)
                // Esto limpia la pila para que no puedas volver atrás a la tabla
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish() // Cierra la actividad de la tabla

            }, 2000)
        }
    }

    private fun guardarPuntuacionEnBD(puntos: Int) {
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombreAlumno = prefs.getString("nombre_alumno_actual", "Anonimo") ?: "Anonimo"

        val dbHelper = DatabaseHelper(this)
        dbHelper.guardarPuntuacion(nombreAlumno, "TxakurraTabla", puntos)
    }
}