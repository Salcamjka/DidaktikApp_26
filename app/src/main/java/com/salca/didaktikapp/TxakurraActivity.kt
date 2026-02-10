package com.salca.didaktikapp

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide

/**
 * Actividad correspondiente a la parada "Txakurraren Iturria" (La Fuente del Perro).
 * Lógica ajustada: Si el usuario tiene la mitad o más respuestas mal (5 aciertos o menos),
 * sale el león triste. Necesita 6 aciertos para ver al feliz.
 */
class TxakurraActivity : AppCompatActivity() {

    // --- Variables de Interfaz (Intro) ---
    private lateinit var contenedorIntro: LinearLayout
    private lateinit var tvTextoIntro1: TextView
    private lateinit var tvTextoIntro2: TextView
    private lateinit var tvLeerMas: TextView
    private lateinit var ivLeonExplicacion: ImageView
    private lateinit var btnPlayPauseIcon: ImageButton
    private lateinit var seekBarAudio: SeekBar
    private lateinit var btnContinuar: Button
    private var textoDesplegado = false

    // --- Variables de Interfaz (Juego) ---
    private lateinit var contenedorTabla: LinearLayout
    private lateinit var btnFinish: Button
    private lateinit var ivGifResultado: ImageView

    // Lista de todos los campos de texto (EditText)
    private lateinit var inputs: List<EditText>

    // Mapa de respuestas correctas
    private val respuestasCorrectas = mapOf(
        R.id.etTxakurra1 to listOf("kanidoa", "kanido", "txakurra"),
        R.id.etTxakurra2 to listOf("etxea", "etxekoa", "basa", "kale"),
        R.id.etTxakurra3 to listOf("orojalea", "piensoa"),
        R.id.etTxakurra4 to listOf("zaunka", "ausiki"),
        R.id.etTxakurra5 to listOf("txikia", "ertaina"),

        R.id.etLehoia1 to listOf("felidoa", "felido"),
        R.id.etLehoia2 to listOf("sabana", "afrika", "oihana"),
        R.id.etLehoia3 to listOf("haragijalea", "haragia"),
        R.id.etLehoia4 to listOf("orrua", "orro"),
        R.id.etLehoia5 to listOf("handia", "oso handia")
    )

    // Variables de estado del juego
    private var aciertos = 0
    private var intentosTotales = 0
    private val TOTAL_PREGUNTAS = 10

    // --- Variables de Audio ---
    private var audio: MediaPlayer? = null
    private val audioHandler = Handler(Looper.getMainLooper())
    private var isPlaying = false

    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            try {
                if (audio != null && isPlaying) {
                    seekBarAudio.progress = audio!!.currentPosition
                }
                audioHandler.postDelayed(this, 500)
            } catch (e: Exception) { }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_txakurra)

        try {
            inicializarVistas()

            // Comprobar Accesibilidad (Texto Grande)
            val sharedPref = getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
            val usarTextoGrande = sharedPref.getBoolean("MODO_TEXTO_GRANDE", false)

            if (usarTextoGrande) {
                tvTextoIntro1.textSize = 24f
                tvTextoIntro2.textSize = 24f
                tvLeerMas.textSize = 22f
                btnContinuar.textSize = 24f
                findViewById<TextView>(R.id.tvTituloTabla)?.textSize = 34f
                findViewById<TextView>(R.id.tvLabelTxakurra)?.textSize = 22f
                findViewById<TextView>(R.id.tvLabelLehoia)?.textSize = 22f
                findViewById<TextView>(R.id.tvInstruccionTabla)?.textSize = 22f
                for (input in inputs) {
                    input.textSize = 20f
                }
                btnFinish.textSize = 24f
            }

            configurarAudio()
            configurarLogicaJuego()

        } catch (e: Exception) {
            Toast.makeText(this, "Errorea: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun inicializarVistas() {
        contenedorIntro = findViewById(R.id.contenedorIntro)
        tvTextoIntro1 = findViewById(R.id.tvTextoIntro1)
        tvTextoIntro2 = findViewById(R.id.tvTextoIntro2)
        tvLeerMas = findViewById(R.id.tvLeerMas)
        ivLeonExplicacion = findViewById(R.id.ivLeonExplicacion)
        btnPlayPauseIcon = findViewById(R.id.btnPlayPauseIcon)
        seekBarAudio = findViewById(R.id.seekBarAudio)
        btnContinuar = findViewById(R.id.btnContinuar)

        contenedorTabla = findViewById(R.id.contenedorTabla)
        btnFinish = findViewById(R.id.btnFinish)
        ivGifResultado = findViewById(R.id.ivGifResultado)

        inputs = listOf(
            findViewById(R.id.etTxakurra1), findViewById(R.id.etTxakurra2),
            findViewById(R.id.etTxakurra3), findViewById(R.id.etTxakurra4),
            findViewById(R.id.etTxakurra5), findViewById(R.id.etLehoia1),
            findViewById(R.id.etLehoia2), findViewById(R.id.etLehoia3),
            findViewById(R.id.etLehoia4), findViewById(R.id.etLehoia5)
        )

        findViewById<ImageButton>(R.id.btnVolverMapa).setOnClickListener {
            if (isPlaying) pauseAudio()
            finish()
        }

        tvLeerMas.setOnClickListener {
            if (!textoDesplegado) {
                tvTextoIntro2.visibility = View.VISIBLE
                tvLeerMas.text = "Irakurri gutxiago ▲"
                ivLeonExplicacion.visibility = View.GONE
                textoDesplegado = true
            } else {
                tvTextoIntro2.visibility = View.GONE
                tvLeerMas.text = "Irakurri gehiago ▼"
                ivLeonExplicacion.visibility = View.VISIBLE
                textoDesplegado = false
            }
        }

        btnContinuar.setOnClickListener {
            if (isPlaying) pauseAudio()
            contenedorIntro.visibility = View.GONE
            contenedorTabla.visibility = View.VISIBLE
        }

        btnFinish.setOnClickListener {
            guardarPuntuacion()
            finish()
        }
    }

    private fun configurarLogicaJuego() {
        for (input in inputs) {
            input.setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) validarCampo(input)
            }
            input.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    validarCampo(input)
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun validarCampo(editText: EditText) {
        if (!editText.isEnabled) return

        val textoEscrito = editText.text.toString().trim().lowercase()
        if (textoEscrito.isEmpty()) return

        val id = editText.id
        val respuestasPosibles = respuestasCorrectas[id] ?: emptyList()

        if (respuestasPosibles.contains(textoEscrito)) {
            editText.setTextColor(Color.parseColor("#006400")) // Verde oscuro
            aciertos++
        } else {
            editText.setTextColor(Color.RED)
        }

        editText.isEnabled = false
        editText.isFocusable = false
        editText.setBackgroundColor(Color.parseColor("#E0E0E0"))

        intentosTotales++
        verificarFinalizacion()
    }

    // -----------------------------------------------------------------------
    // AQUÍ ESTÁ EL CAMBIO IMPORTANTE
    // -----------------------------------------------------------------------
    private fun verificarFinalizacion() {
        if (intentosTotales == TOTAL_PREGUNTAS) {
            btnFinish.isEnabled = true
            btnFinish.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.txakurra))

            val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
            val nombreUsuario = prefs.getString("nombre_alumno_actual", "") ?: ""
            prefs.edit().putBoolean("completado_txakurra_$nombreUsuario", true).apply()

            ivGifResultado.visibility = View.VISIBLE

            // LÓGICA: "Mitad o más mal" -> Triste.
            // Mitad de 10 es 5. Si tiene 5 aciertos, significa que tiene 5 fallos (la mitad mal).
            // Por tanto:
            // Si aciertos > 5 (6, 7, 8, 9, 10) -> FELIZ
            // Si aciertos <= 5 (0, 1, 2, 3, 4, 5) -> TRISTE

            val gifResId = if (aciertos > 5) {
                R.drawable.leonfeliz
            } else {
                R.drawable.leontriste
            }

            try {
                Glide.with(this).asGif().load(gifResId).into(ivGifResultado)
            } catch (e: Exception) {
                // Fallback por si falla Glide o no es un gif
                ivGifResultado.setImageResource(gifResId)
            }
        }
    }

    private fun guardarPuntuacion() {
        val puntos = aciertos * 10
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombre = prefs.getString("nombre_alumno_actual", "Anonimo") ?: "Anonimo"
        val dbHelper = DatabaseHelper(this)
        dbHelper.guardarPuntuacion(nombre, "Txakurra", puntos)
        SyncHelper.subirInmediatamente(this)
    }

    private fun configurarAudio() {
        if (audio == null) {
            audio = MediaPlayer.create(this, R.raw.jarduera_4)
            audio?.setOnCompletionListener {
                pauseAudio()
                audio?.seekTo(0)
                seekBarAudio.progress = 0
            }
            if (audio != null) {
                seekBarAudio.max = audio!!.duration
            }
        }

        btnPlayPauseIcon.setOnClickListener {
            if (isPlaying) pauseAudio() else playAudio()
        }

        seekBarAudio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) audio?.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                audioHandler.removeCallbacks(updateSeekBarRunnable)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (isPlaying) audioHandler.postDelayed(updateSeekBarRunnable, 500)
            }
        })
    }

    private fun playAudio() {
        audio?.start()
        isPlaying = true
        btnPlayPauseIcon.setImageResource(android.R.drawable.ic_media_pause)
        audioHandler.post(updateSeekBarRunnable)
    }

    private fun pauseAudio() {
        audio?.pause()
        isPlaying = false
        btnPlayPauseIcon.setImageResource(android.R.drawable.ic_media_play)
        audioHandler.removeCallbacks(updateSeekBarRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        audioHandler.removeCallbacks(updateSeekBarRunnable)
        audio?.release()
        audio = null
    }
}