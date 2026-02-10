package com.salca.didaktikapp

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide

/**
 * Actividad que implementa el juego del Ahorcado (Urkatua).
 *
 * En esta pantalla, el usuario debe adivinar una palabra relacionada con la historia de Bilbao
 * (en este caso, el Escudo del Athletic y San Antón).
 *
 * Funcionalidades principales:
 * * Generación dinámica del teclado A-Z.
 * * Control de errores (máximo 7) y cambios de imagen del ahorcado.
 * * Cálculo de puntuación basado en aciertos, fallos y tiempo/vidas restantes.
 * * Fase de explicación con audio y texto tras finalizar la partida.
 * * Adaptación de accesibilidad (tamaño de texto).
 *
 * @author Marco
 * @version 1.0
 */
class AhorcadoActivity : AppCompatActivity() {

    // --- Variables de la Interfaz de Usuario (UI) ---
    private lateinit var contenedorFaseJuego: View
    private lateinit var ivAhorcado: ImageView
    private lateinit var tvPalabra: TextView
    private lateinit var tvResultado: TextView
    private lateinit var llTeclado: LinearLayout
    private lateinit var btnJarraituJuego: Button

    // Variable para mostrar el GIF de victoria o derrota
    private lateinit var ivGifResultado: ImageView

    // Botón para volver al mapa
    private lateinit var btnVolverMapa: ImageButton

    // --- Variables de la Lógica del Juego ---
    /** Lista de palabras posibles para el juego. */
    private val palabras = listOf("ATHLETIC-EN ARMARRIA")
    /** Palabra que se está jugando actualmente. */
    private var palabraActual = ""
    /** Lista de caracteres que el usuario ya ha acertado. */
    private var letrasAdivinadas = mutableListOf<Char>()
    /** Contador de fallos. */
    private var errores = 0
    /** Puntuación acumulada en la partida. */
    private var puntuacionActual = 0

    // --- Variables de la Fase de Explicación (Audio) ---
    private lateinit var contenedorFaseExplicacion: View
    private lateinit var btnPlayPause: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var btnJarraituExplicacion: Button
    private var mediaPlayer: MediaPlayer? = null
    private val handlerAudio = Handler(Looper.getMainLooper())

    /**
     * Método de inicio de la actividad.
     *
     * Configura la orientación vertical, carga el diseño, aplica ajustes de accesibilidad
     * e inicia la lógica del juego.
     *
     * @param savedInstanceState Estado guardado de la instancia.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 1. Bloqueo de orientación ---
        // Forzamos que la pantalla se quede en vertical para evitar reajustes del teclado
        try {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } catch (e: Exception) {}

        setContentView(R.layout.activity_ahorcado)

        // --- 2. Inicializar Vistas ---
        inicializarVistas()

        // --- 3. Aplicar Accesibilidad (Texto Grande) ---
        val sharedPref = getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
        val usarTextoGrande = sharedPref.getBoolean("MODO_TEXTO_GRANDE", false)

        if (usarTextoGrande) {
            findViewById<TextView>(R.id.tvTituloCabecera)?.textSize = 34f
            tvResultado.textSize = 30f
            findViewById<TextView>(R.id.tvTextoExplicativo)?.textSize = 24f
            btnJarraituJuego.textSize = 22f
            btnJarraituExplicacion.textSize = 22f
        }

        // --- 4. Arrancar el juego ---
        iniciarJuego()
    }

    /**
     * Vincula las variables con los elementos del XML y configura los Listeners.
     */
    private fun inicializarVistas() {
        btnVolverMapa = findViewById(R.id.btnVolverMapa)
        btnVolverMapa.setOnClickListener {
            // Si el audio está sonando al salir, lo paramos
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            finish()
        }

        contenedorFaseJuego = findViewById(R.id.fase1_Juego)
        ivAhorcado = findViewById(R.id.ivAhorcado)
        tvPalabra = findViewById(R.id.tvPalabra)
        tvResultado = findViewById(R.id.tvResultado)
        llTeclado = findViewById(R.id.llTeclado)

        ivGifResultado = findViewById(R.id.ivGifResultado)

        btnJarraituJuego = findViewById(R.id.btnJarraituJuego)

        // Configuración inicial del botón "Continuar" (desactivado hasta terminar)
        btnJarraituJuego.isEnabled = false
        val colorDesactivado = ContextCompat.getColor(this, R.color.boton_desactivado)
        btnJarraituJuego.backgroundTintList = ColorStateList.valueOf(colorDesactivado)

        // Al pulsar continuar, pasamos a la fase de explicación
        btnJarraituJuego.setOnClickListener { mostrarFaseExplicacion() }

        // Elementos de la fase de explicación
        contenedorFaseExplicacion = findViewById(R.id.fase_Explicacion)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        seekBar = findViewById(R.id.seekBarAudio)
        btnJarraituExplicacion = findViewById(R.id.btnJarraituExplicacion)

        btnJarraituExplicacion.setOnClickListener {
            SyncHelper.subirInmediatamente(this) // Sincronizar datos al salir
            finish()
        }

        btnPlayPause.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) pausarAudio() else reproducirAudio()
        }

        // Barra de progreso del audio
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    /**
     * Resetea las variables y prepara el tablero para una nueva partida.
     */
    private fun iniciarJuego() {
        errores = 0
        puntuacionActual = 0
        letrasAdivinadas.clear()
        palabraActual = palabras.random() // Elegir palabra al azar

        // Gestión de visibilidad de capas
        contenedorFaseJuego.visibility = View.VISIBLE
        contenedorFaseExplicacion.visibility = View.GONE
        tvResultado.visibility = View.GONE
        ivGifResultado.visibility = View.GONE

        llTeclado.visibility = View.VISIBLE
        btnVolverMapa.visibility = View.VISIBLE

        // Resetear imagen del ahorcado a la inicial (sin fallos)
        ivAhorcado.setImageResource(R.drawable.ahorcado0)

        // Resetear botón de continuar
        btnJarraituJuego.isEnabled = false
        val colorDesactivado = ContextCompat.getColor(this, R.color.boton_desactivado)
        btnJarraituJuego.backgroundTintList = ColorStateList.valueOf(colorDesactivado)

        crearTeclado()
        actualizarTextoPantalla()
    }

    /**
     * Cambia la interfaz de la fase de juego a la fase educativa/explicación.
     */
    private fun mostrarFaseExplicacion() {
        contenedorFaseJuego.visibility = View.GONE
        contenedorFaseExplicacion.visibility = View.VISIBLE
        btnVolverMapa.visibility = View.GONE
        configurarAudio()
    }

    /**
     * Genera dinámicamente los botones del teclado (A-Z) en filas de 7.
     */
    private fun crearTeclado() {
        llTeclado.removeAllViews()
        var contador = 0
        var filaActual: LinearLayout? = null

        val marginPx = (2 * resources.displayMetrics.density).toInt()

        for (letra in 'A'..'Z') {
            // Cada 7 letras, creamos una nueva fila horizontal
            if (contador % 7 == 0) {
                filaActual = LinearLayout(this)
                filaActual.orientation = LinearLayout.HORIZONTAL
                filaActual.gravity = Gravity.CENTER
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, marginPx * 2)
                filaActual.layoutParams = params
                llTeclado.addView(filaActual)
            }

            // Crear el botón de la letra
            val boton = Button(this)
            boton.text = letra.toString()
            boton.textSize = 14f

            // Ajustes visuales para que entren 7 botones
            boton.setPadding(0,0,0,0)
            boton.minWidth = 0
            boton.minimumWidth = 0
            boton.minHeight = 0
            boton.minimumHeight = 0

            val paramsBtn = LinearLayout.LayoutParams(
                0,
                120,
                1.0f // Peso 1 para repartir espacio equitativamente
            )
            paramsBtn.setMargins(marginPx, 0, marginPx, 0)

            boton.layoutParams = paramsBtn
            boton.setOnClickListener { procesarLetra(letra, boton) }
            filaActual?.addView(boton)
            contador++
        }

        // Rellenar huecos vacíos en la última fila para mantener alineación
        val letrasRestantes = 7 - (contador % 7)
        if (letrasRestantes < 7 && filaActual != null) {
            for (i in 0 until letrasRestantes) {
                val hueco = View(this)
                val paramsHueco = LinearLayout.LayoutParams(0, 120, 1.0f)
                paramsHueco.setMargins(marginPx, 0, marginPx, 0)
                hueco.layoutParams = paramsHueco
                filaActual.addView(hueco)
            }
        }
    }

    /**
     * Lógica al pulsar una letra del teclado.
     *
     * @param letra Carácter pulsado.
     * @param boton Referencia al botón para desactivarlo o cambiarle el color.
     */
    private fun procesarLetra(letra: Char, boton: Button) {
        if (palabraActual.contains(letra)) {
            // --- ACIERTO (VERDE) ---
            if (!letrasAdivinadas.contains(letra)) {
                letrasAdivinadas.add(letra)
                puntuacionActual += 10
            }
            boton.isEnabled = false
            boton.setBackgroundColor(ContextCompat.getColor(this, R.color.mi_acierto))
            actualizarTextoPantalla()
            verificarVictoria()
        } else {
            // --- FALLO (GRIS) ---
            errores++
            puntuacionActual -= 5
            if (puntuacionActual < 0) puntuacionActual = 0

            boton.isEnabled = false
            boton.setBackgroundColor(android.graphics.Color.GRAY)

            actualizarImagenAhorcado()
            if (errores >= 7) mostrarResultadoFinal(gano = false)
        }
    }

    /**
     * Actualiza el TextView principal mostrando guiones bajos o las letras acertadas.
     */
    private fun actualizarTextoPantalla() {
        val sb = StringBuilder()
        for (c in palabraActual) {
            if (c == ' ') sb.append("\n") else {
                if (letrasAdivinadas.contains(c) || c == '-') sb.append("$c ") else sb.append("_ ")
            }
        }
        tvPalabra.text = sb.toString()
    }

    /**
     * Comprueba si se han adivinado todas las letras de la palabra.
     */
    private fun verificarVictoria() {
        var gano = true
        for (c in palabraActual) {
            if (c != '-' && c != ' ') {
                if (!letrasAdivinadas.contains(c)) {
                    gano = false
                    break
                }
            }
        }
        if (gano) mostrarResultadoFinal(gano = true)
    }

    /**
     * Cambia la imagen central según el número de errores cometidos (0 a 6).
     */
    private fun actualizarImagenAhorcado() {
        val res = when (errores) {
            0 -> R.drawable.ahorcado0
            1 -> R.drawable.ahorcado1
            2 -> R.drawable.ahorcado2
            3 -> R.drawable.ahorcado3
            4 -> R.drawable.ahorcado4
            5 -> R.drawable.ahorcado5
            else -> R.drawable.ahorcado6
        }
        ivAhorcado.setImageResource(res)
    }

    /**
     * Gestiona el final del juego (Victoria o Derrota).
     *
     * Muestra el mensaje correspondiente, el GIF animado, calcula la puntuación final
     * y guarda el progreso.
     *
     * @param gano true si el usuario acertó la palabra, false si se quedó sin vidas.
     */
    private fun mostrarResultadoFinal(gano: Boolean) {
        // Ocultamos el teclado siempre al finalizar
        llTeclado.visibility = View.GONE

        tvResultado.visibility = View.VISIBLE

        val gifResId: Int

        if (gano) {
            // Cálculo de bonus
            val bonusVictoria = 120
            val vidasRestantes = 7 - errores
            val bonusVidas = vidasRestantes * 40
            puntuacionActual += (bonusVictoria + bonusVidas)

            tvResultado.text = "OSO ONDO! IRABAZI DUZU!"
            tvResultado.setTextColor(ContextCompat.getColor(this, R.color.mi_acierto))
            ivAhorcado.setImageResource(R.drawable.escudo) // Mostrar escudo al ganar

            gifResId = R.drawable.leonfeliz

            // Marcar actividad como completada en SharedPreferences
            val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
            val nombreUsuario = prefs.getString("nombre_alumno_actual", "") ?: ""
            prefs.edit().putBoolean("completado_ahorcado_$nombreUsuario", true).apply()

        } else {
            tvResultado.text = "GALDU DUZU... HITZA: $palabraActual"
            tvResultado.setTextColor(ContextCompat.getColor(this, R.color.mi_error_texto))
            ivAhorcado.setImageResource(R.drawable.escudo)

            gifResId = R.drawable.leontriste
        }

        // --- Cargar GIF con Glide ---
        ivGifResultado.visibility = View.VISIBLE
        try {
            Glide.with(this).asGif().load(gifResId).into(ivGifResultado)
        } catch (e: Exception) {
            ivGifResultado.setImageResource(gifResId)
        }

        guardarPuntuacionEnBD(puntuacionActual)

        // Activar botón para ir a la explicación
        btnJarraituJuego.isEnabled = true
        val colorActivo = ContextCompat.getColor(this, R.color.ahorcado)
        btnJarraituJuego.backgroundTintList = ColorStateList.valueOf(colorActivo)
        btnJarraituJuego.setTextColor(android.graphics.Color.WHITE)
    }

    /**
     * Guarda la puntuación obtenida en la base de datos local y sincroniza.
     */
    private fun guardarPuntuacionEnBD(puntos: Int) {
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombreAlumno = prefs.getString("nombre_alumno_actual", "Anonimo") ?: "Anonimo"
        val dbHelper = DatabaseHelper(this)
        dbHelper.guardarPuntuacion(nombreAlumno, "Ahorcado", puntos)
        SyncHelper.subirInmediatamente(this)
    }

    // ================================================================
    // MÉTODOS DE AUDIO
    // ================================================================

    /** Inicializa el MediaPlayer con el archivo de audio. */
    private fun configurarAudio() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.jarduera_1) // Asegúrate de que este archivo existe
            mediaPlayer?.let { seekBar.max = it.duration }
            mediaPlayer?.setOnCompletionListener {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                seekBar.progress = 0
                handlerAudio.removeCallbacks(actualizarSeekBarRunnable)
            }
        }
    }

    /** Reproduce el audio y actualiza el icono y la barra de progreso. */
    private fun reproducirAudio() {
        mediaPlayer?.start()
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        handlerAudio.postDelayed(actualizarSeekBarRunnable, 0)
    }

    /** Pausa el audio. */
    private fun pausarAudio() {
        mediaPlayer?.pause()
        btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
        handlerAudio.removeCallbacks(actualizarSeekBarRunnable)
    }

    /** Runnable encargado de actualizar la posición de la SeekBar cada 500ms. */
    private val actualizarSeekBarRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                seekBar.progress = it.currentPosition
                handlerAudio.postDelayed(this, 500)
            }
        }
    }

    /** Limpieza de recursos al destruir la actividad. */
    override fun onDestroy() {
        super.onDestroy()
        handlerAudio.removeCallbacks(actualizarSeekBarRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}