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
 *
 * Esta actividad plantea una dinámica diferente:
 * 1. **Fase Educativa:** Explicación sobre la fuente, sus leones (que parecen perros) y su historia.
 * 2. **Fase de Juego:** Una tabla comparativa donde el alumno debe escribir características
 * que diferencian a un Perro (Txakurra) de un León (Lehoia).
 *
 * Características técnicas:
 * * Validación de texto escrito por el usuario.
 * * Gestión de foco y teclado (IME Actions).
 * * Feedback visual inmediato (Texto verde/rojo).
 *
 * @author Salca
 * @version 1.0
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

    // Mapa de respuestas correctas: ID del campo -> Lista de palabras válidas
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

    // Runnable para actualizar la barra de progreso del audio
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

    /**
     * Método de inicio de la actividad.
     *
     * Carga el layout, aplica accesibilidad si es necesario e inicializa la lógica de validación.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_txakurra)

        try {
            // 1. Inicializar vistas
            inicializarVistas()

            // 2. Comprobar Accesibilidad (Texto Grande)
            val sharedPref = getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
            val usarTextoGrande = sharedPref.getBoolean("MODO_TEXTO_GRANDE", false)

            if (usarTextoGrande) {
                // Aumentar tamaños de fuente manualmente
                tvTextoIntro1.textSize = 24f
                tvTextoIntro2.textSize = 24f
                tvLeerMas.textSize = 22f
                btnContinuar.textSize = 24f

                findViewById<TextView>(R.id.tvTituloTabla)?.textSize = 34f
                findViewById<TextView>(R.id.tvLabelTxakurra)?.textSize = 22f
                findViewById<TextView>(R.id.tvLabelLehoia)?.textSize = 22f
                findViewById<TextView>(R.id.tvInstruccionTabla)?.textSize = 22f

                // Ajustar todos los cuadros de texto
                for (input in inputs) {
                    input.textSize = 20f
                }

                btnFinish.textSize = 24f
            }

            // 3. Configurar lógica
            configurarAudio()
            configurarLogicaJuego()

        } catch (e: Exception) {
            Toast.makeText(this, "Errorea: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Vincula las variables con los elementos del XML.
     */
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

        // Lista de los 10 campos editables (5 perro + 5 león)
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

        // Expandir/Colapsar texto de introducción
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

        // Pasar de la intro al juego
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

    /**
     * Configura los Listeners para cada campo de texto.
     * Se valida la respuesta cuando:
     * 1. El usuario cambia el foco a otro campo.
     * 2. El usuario pulsa "Intro" o "Siguiente" en el teclado.
     */
    private fun configurarLogicaJuego() {
        for (input in inputs) {
            // Listener de Foco (cuando pinchas fuera)
            input.setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) validarCampo(input)
            }
            // Listener de Teclado (Done/Next)
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

    /**
     * Valida si el texto introducido es correcto comparándolo con el mapa `respuestasCorrectas`.
     *
     * @param editText El campo de texto a validar.
     */
    private fun validarCampo(editText: EditText) {
        if (!editText.isEnabled) return // Si ya está validado, salimos

        val textoEscrito = editText.text.toString().trim().lowercase()
        if (textoEscrito.isEmpty()) return // Si está vacío, no hacemos nada

        val id = editText.id
        val respuestasPosibles = respuestasCorrectas[id] ?: emptyList()

        if (respuestasPosibles.contains(textoEscrito)) {
            // ACIERTO: Texto verde oscuro
            editText.setTextColor(Color.parseColor("#006400"))
            aciertos++
        } else {
            // FALLO: Texto rojo
            editText.setTextColor(Color.RED)
        }

        // Bloqueamos el campo para que no se pueda editar más
        editText.isEnabled = false
        editText.isFocusable = false
        editText.setBackgroundColor(Color.parseColor("#E0E0E0")) // Fondo grisáceo

        intentosTotales++
        verificarFinalizacion()
    }

    /**
     * Comprueba si se han respondido las 10 preguntas.
     */
    private fun verificarFinalizacion() {
        if (intentosTotales == TOTAL_PREGUNTAS) {
            // Habilitar botón de finalizar
            btnFinish.isEnabled = true
            btnFinish.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.txakurra))

            // Guardar estado completado
            val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
            val nombreUsuario = prefs.getString("nombre_alumno_actual", "") ?: ""
            prefs.edit().putBoolean("completado_txakurra_$nombreUsuario", true).apply()

            // Mostrar GIF según rendimiento
            ivGifResultado.visibility = View.VISIBLE
            val gifResId = if (aciertos >= 5) { // Aprobado
                R.drawable.leonfeliz
            } else { // Suspenso
                R.drawable.leontriste
            }

            try {
                Glide.with(this).asGif().load(gifResId).into(ivGifResultado)
            } catch (e: Exception) {
                ivGifResultado.setImageResource(gifResId)
            }

            // Guardar automáticamente al terminar
            guardarPuntuacion()
        }
    }

    /**
     * Guarda la puntuación en la BD y sincroniza con el servidor.
     */
    private fun guardarPuntuacion() {
        val puntos = aciertos * 10
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombre = prefs.getString("nombre_alumno_actual", "Anonimo") ?: "Anonimo"
        val dbHelper = DatabaseHelper(this)
        dbHelper.guardarPuntuacion(nombre, "Txakurra", puntos)
        SyncHelper.subirInmediatamente(this)
    }

    // ================================================================
    // MÉTODOS DE AUDIO
    // ================================================================

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