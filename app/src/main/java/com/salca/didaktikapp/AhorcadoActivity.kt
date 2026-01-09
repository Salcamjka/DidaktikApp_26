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

class AhorcadoActivity : AppCompatActivity() {

    // --- Variables Fase JUEGO ---
    private lateinit var contenedorFaseJuego: View
    private lateinit var layoutJuego: LinearLayout
    private lateinit var ivAhorcado: ImageView
    private lateinit var tvPalabra: TextView
    private lateinit var llTeclado: LinearLayout
    private lateinit var layoutFinal: LinearLayout
    private lateinit var ivFinalSuperior: ImageView
    private lateinit var tvMensajePrincipal: TextView
    private lateinit var tvMensajeSecundario: TextView
    private lateinit var ivFinalInferior: ImageView
    private lateinit var btnJarraituJuego: Button

    private val palabras = listOf("ATHLETIC-EN ARMARRIA")
    private var palabraActual = ""
    private var letrasAdivinadas = mutableListOf<Char>()
    private var errores = 0

    // --- Variables Fase EXPLICACIÓN ---
    private lateinit var contenedorFaseExplicacion: View
    private lateinit var btnPlayPause: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var btnJarraituExplicacion: Button

    // Audio
    private var mediaPlayer: MediaPlayer? = null
    private val handlerAudio = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ahorcado_activity)

        inicializarVistas()
        iniciarJuego()
    }

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

        // Al terminar el juego, pasamos a la explicación
        btnJarraituJuego.setOnClickListener {
            mostrarFaseExplicacion()
        }

        // --- EXPLICACIÓN ---
        contenedorFaseExplicacion = findViewById(R.id.fase_Explicacion)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        seekBar = findViewById(R.id.seekBarAudio)
        btnJarraituExplicacion = findViewById(R.id.btnJarraituExplicacion)

        // Botón final: Cierra la actividad
        btnJarraituExplicacion.setOnClickListener {
            finish()
        }

        // Control del Reproductor
        btnPlayPause.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) pausarAudio() else reproducirAudio()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    // ==========================================
    // TRANSICIÓN A FASE EXPLICACIÓN
    // ==========================================

    private fun mostrarFaseExplicacion() {
        // Ocultamos el juego
        contenedorFaseJuego.visibility = View.GONE

        // Mostramos la explicación (Audio + Texto juntos)
        contenedorFaseExplicacion.visibility = View.VISIBLE

        // Preparamos el audio para que esté listo para darle al Play
        configurarAudio()
    }

    // ==========================================
    // LÓGICA DEL JUEGO
    // ==========================================
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

    private fun procesarLetra(letra: Char, boton: Button) {
        if (palabraActual.contains(letra)) {
            if (!letrasAdivinadas.contains(letra)) letrasAdivinadas.add(letra)
            boton.isEnabled = false
            boton.setBackgroundColor(Color.parseColor("#4CAF50"))
            actualizarTextoPantalla()
            verificarVictoria()
        } else {
            errores++
            actualizarImagenAhorcado()
            if (errores >= 7) mostrarPantallaFinal(gano = false)
        }
    }

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

    private fun mostrarPantallaFinal(gano: Boolean) {
        layoutJuego.visibility = View.GONE
        layoutFinal.visibility = View.VISIBLE
        if (gano) {
            ivFinalSuperior.visibility = View.VISIBLE
            ivFinalSuperior.setImageResource(R.drawable.escudo)
            tvMensajePrincipal.text = palabraActual
            tvMensajePrincipal.setTextColor(Color.BLACK)
            tvMensajeSecundario.visibility = View.VISIBLE
            tvMensajeSecundario.text = "Oso ondo, irabazi duzu!"
            ivFinalInferior.setImageResource(R.drawable.leonfeliz)
            btnJarraituJuego.visibility = View.VISIBLE
        } else {
            ivFinalSuperior.visibility = View.GONE
            tvMensajeSecundario.visibility = View.GONE
            tvMensajePrincipal.text = "Galdu duzu.\nHitza hau zen:\n$palabraActual"
            tvMensajePrincipal.setTextColor(Color.RED)
            ivFinalInferior.setImageResource(R.drawable.leontriste)
        }
    }

    // ==========================================
    // LÓGICA DE AUDIO
    // ==========================================
    private fun configurarAudio() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.audioahorcado)
            mediaPlayer?.let { seekBar.max = it.duration }
            mediaPlayer?.setOnCompletionListener {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                seekBar.progress = 0
                handlerAudio.removeCallbacks(actualizarSeekBarRunnable)
            }
        }
    }

    private fun reproducirAudio() {
        mediaPlayer?.start()
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        handlerAudio.postDelayed(actualizarSeekBarRunnable, 0)
    }

    private fun pausarAudio() {
        mediaPlayer?.pause()
        btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
        handlerAudio.removeCallbacks(actualizarSeekBarRunnable)
    }

    private val actualizarSeekBarRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                seekBar.progress = it.currentPosition
                handlerAudio.postDelayed(this, 500)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handlerAudio.removeCallbacks(actualizarSeekBarRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}