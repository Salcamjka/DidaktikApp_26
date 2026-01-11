package com.salca.didaktikapp

import android.annotation.SuppressLint
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.salca.didaktikapp.R

/**
 * Actividad principal del juego del Ahorcado.
 *
 * Esta clase gestiona tanto la lógica del juego como la pantalla de explicación
 * cultural que aparece al finalizar.
 *
 * @author Marco
 * @version 1.0
 */
class AhorcadoActivity : AppCompatActivity() {

    // ==========================================
    // VARIABLES DE LA FASE DE JUEGO
    // ==========================================

    /** Contenedor principal de la pantalla del juego. */
    private lateinit var contenedorFaseJuego: View
    /** Layout con los elementos visuales de la partida. */
    private lateinit var layoutJuego: LinearLayout
    /** Imagen que muestra el estado del muñeco ahorcado. */
    private lateinit var ivAhorcado: ImageView
    /** TextView donde se pintan los guiones y las letras acertadas. */
    private lateinit var tvPalabra: TextView
    /** Contenedor para los botones del teclado generado por código. */
    private lateinit var llTeclado: LinearLayout

    /** Layout de la pantalla final (Ganar/Perder). */
    private lateinit var layoutFinal: LinearLayout
    /** Imagen del Escudo (visible siempre al final). */
    private lateinit var ivFinalSuperior: ImageView
    /** Mensaje principal (La palabra correcta o mensaje de error). */
    private lateinit var tvMensajePrincipal: TextView
    /** Mensaje secundario ("Has ganado"). */
    private lateinit var tvMensajeSecundario: TextView
    /** Imagen inferior (León feliz o triste). */
    private lateinit var ivFinalInferior: ImageView
    /** Botón para avanzar a la explicación. */
    private lateinit var btnJarraituJuego: Button

    /** Lista de palabras para el juego. */
    private val palabras = listOf("ATHLETIC-EN ARMARRIA")
    /** Palabra actual que se está jugando. */
    private var palabraActual = ""
    /** Lista de letras que el usuario ha acertado. */
    private var letrasAdivinadas = mutableListOf<Char>()
    /** Contador de fallos. */
    private var errores = 0

    // ==========================================
    // VARIABLES DE LA FASE DE EXPLICACIÓN
    // ==========================================

    /** Contenedor de la pantalla de explicación (Texto + Audio). */
    private lateinit var contenedorFaseExplicacion: View
    /** Botón para Play/Pause del audio. */
    private lateinit var btnPlayPause: ImageButton
    /** Barra de progreso del audio. */
    private lateinit var seekBar: SeekBar
    /** Botón para salir de la actividad. */
    private lateinit var btnJarraituExplicacion: Button

    /** Reproductor de audio. */
    private var mediaPlayer: MediaPlayer? = null
    /** Handler para actualizar la barra de progreso en segundo plano. */
    private val handlerAudio = Handler(Looper.getMainLooper())

    /**
     * Se ejecuta al iniciar la actividad.
     * Carga el layout y arranca el juego.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ahorcado) // Asegúrate que el XML se llama así

        inicializarVistas()
        iniciarJuego()
    }

    /**
     * Vincula las variables con los IDs del XML y configura los botones.
     */
    private fun inicializarVistas() {
        // --- JUEGO ---
        contenedorFaseJuego = findViewById(R.id.fase1_Juego)
        layoutJuego = findViewById(R.id.layoutJuego)
        ivAhorcado = findViewById(R.id.ivAhorcado)
        tvPalabra = findViewById(R.id.tvPalabra)
        llTeclado = findViewById(R.id.llTeclado)
        layoutFinal = findViewById(R.id.layoutFinal)
        ivFinalSuperior = findViewById(R.id.ivFinalSuperior)
        tvMensajePrincipal = findViewById(R.id.tvMensajePrincipal)
        tvMensajeSecundario = findViewById(R.id.tvMensajeSecundario)
        ivFinalInferior = findViewById(R.id.ivFinalInferior)
        btnJarraituJuego = findViewById(R.id.btnJarraituJuego)

        // Botón "Jarraitu" del juego -> Ir a explicación
        btnJarraituJuego.setOnClickListener {
            mostrarFaseExplicacion()
        }

        // --- EXPLICACIÓN ---
        contenedorFaseExplicacion = findViewById(R.id.fase_Explicacion)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        seekBar = findViewById(R.id.seekBarAudio)
        btnJarraituExplicacion = findViewById(R.id.btnJarraituExplicacion)

        // Botón "Jarraitu" final -> Cerrar
        btnJarraituExplicacion.setOnClickListener {
            finish()
        }

        // Lógica del botón Play/Pause
        btnPlayPause.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) pausarAudio() else reproducirAudio()
        }

        // Lógica para mover la barra de audio manualmente
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    // ==========================================
    // CAMBIO DE PANTALLA
    // ==========================================

    /**
     * Oculta el juego, muestra la explicación y prepara el audio.
     */
    private fun mostrarFaseExplicacion() {
        contenedorFaseJuego.visibility = View.GONE
        contenedorFaseExplicacion.visibility = View.VISIBLE
        configurarAudio()
    }

    // ==========================================
    // LÓGICA DEL JUEGO
    // ==========================================

    /**
     * Resetea contadores, elige palabra y dibuja el teclado.
     */
    private fun iniciarJuego() {
        errores = 0
        letrasAdivinadas.clear()
        palabraActual = palabras.random()

        contenedorFaseJuego.visibility = View.VISIBLE
        contenedorFaseExplicacion.visibility = View.GONE
        layoutFinal.visibility = View.GONE
        layoutJuego.visibility = View.VISIBLE
        ivAhorcado.setImageResource(R.drawable.ahorcado0)

        crearTeclado()
        actualizarTextoPantalla()
    }

    /**
     * Crea botones de la A a la Z y los añade al layout en filas de 7.
     */
    private fun crearTeclado() {
        llTeclado.removeAllViews()
        var contador = 0
        var filaActual: LinearLayout? = null
        for (letra in 'A'..'Z') {
            if (contador % 7 == 0) {
                filaActual = LinearLayout(this)
                filaActual.orientation = LinearLayout.HORIZONTAL
                filaActual.gravity = Gravity.CENTER
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 5, 0, 5)
                filaActual.layoutParams = params
                llTeclado.addView(filaActual)
            }
            val boton = Button(this)
            boton.text = letra.toString()
            boton.textSize = 14f
            val paramsBtn = LinearLayout.LayoutParams(110, 110)
            paramsBtn.setMargins(5, 0, 5, 0)
            boton.layoutParams = paramsBtn
            boton.setOnClickListener { procesarLetra(letra, boton) }
            filaActual?.addView(boton)
            contador++
        }
    }

    /**
     * Comprueba si la letra pulsada está en la palabra.
     */
    private fun procesarLetra(letra: Char, boton: Button) {
        if (palabraActual.contains(letra)) {
            // Acierto
            if (!letrasAdivinadas.contains(letra)) letrasAdivinadas.add(letra)
            boton.isEnabled = false
            boton.setBackgroundColor(Color.parseColor("#4CAF50"))
            actualizarTextoPantalla()
            verificarVictoria()
        } else {
            // Fallo
            errores++
            actualizarImagenAhorcado()
            if (errores >= 7) mostrarPantallaFinal(gano = false)
        }
    }

    /**
     * Refresca el texto con guiones y letras acertadas.
     */
    private fun actualizarTextoPantalla() {
        val sb = StringBuilder()
        for (c in palabraActual) {
            if (c == ' ') sb.append("\n")
            else {
                if (letrasAdivinadas.contains(c) || c == '-') sb.append("$c ")
                else sb.append("_ ")
            }
        }
        tvPalabra.text = sb.toString()
    }

    /**
     * Verifica si se ha completado la palabra.
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
        if (gano) mostrarPantallaFinal(gano = true)
    }

    /**
     * Actualiza la imagen del muñeco según los errores.
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
     * Muestra la pantalla de resultado final.
     * @param gano true si ganó, false si perdió.
     */
    private fun mostrarPantallaFinal(gano: Boolean) {
        layoutJuego.visibility = View.GONE
        layoutFinal.visibility = View.VISIBLE

        // Escudo visible siempre
        ivFinalSuperior.visibility = View.VISIBLE
        ivFinalSuperior.setImageResource(R.drawable.escudo)

        if (gano) {
            tvMensajePrincipal.text = palabraActual
            tvMensajePrincipal.setTextColor(Color.BLACK)
            tvMensajeSecundario.visibility = View.VISIBLE
            tvMensajeSecundario.text = "Oso ondo, irabazi duzu!"
            ivFinalInferior.setImageResource(R.drawable.leonfeliz)
        } else {
            tvMensajeSecundario.visibility = View.GONE
            tvMensajePrincipal.text = "Galdu duzu.\nHitza hau zen:\n$palabraActual"
            tvMensajePrincipal.setTextColor(Color.RED)
            ivFinalInferior.setImageResource(R.drawable.leontriste)
        }

        // Botón visible para ir a la explicación
        btnJarraituJuego.text = "JARRAITU"
        btnJarraituJuego.visibility = View.VISIBLE
    }

    // ==========================================
    // LÓGICA DE AUDIO
    // ==========================================

    /**
     * Carga el audio y prepara los listeners.
     */
    private fun configurarAudio() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.audioahorcado)
            mediaPlayer?.let { seekBar.max = it.duration }

            // Al terminar, reinicia iconos
            mediaPlayer?.setOnCompletionListener {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                seekBar.progress = 0
                handlerAudio.removeCallbacks(actualizarSeekBarRunnable)
            }
        }
    }

    /** Reproduce el audio. */
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

    /** Hilo secundario para actualizar la barra de progreso. */
    private val actualizarSeekBarRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                seekBar.progress = it.currentPosition
                handlerAudio.postDelayed(this, 500)
            }
        }
    }

    /**
     * Limpia la memoria al cerrar la actividad.
     */
    override fun onDestroy() {
        super.onDestroy()
        handlerAudio.removeCallbacks(actualizarSeekBarRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}