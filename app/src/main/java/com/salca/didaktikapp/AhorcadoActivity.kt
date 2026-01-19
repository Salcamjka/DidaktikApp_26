package com.salca.didaktikapp

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class AhorcadoActivity : AppCompatActivity() {

    // --- Variables UI ---
    private lateinit var contenedorFaseJuego: View
    private lateinit var ivAhorcado: ImageView
    private lateinit var tvPalabra: TextView
    private lateinit var tvResultado: TextView
    private lateinit var llTeclado: LinearLayout
    private lateinit var btnJarraituJuego: Button

    // NUEVO: Variable para el botón del mapa
    private lateinit var btnVolverMapa: ImageButton

    // NUEVO: Variable para la mascota animada
    private lateinit var ivMascotaAnimada: ImageView

    // --- Variables Lógica Juego ---
    private val palabras = listOf("ATHLETIC-EN ARMARRIA")
    private var palabraActual = ""
    private var letrasAdivinadas = mutableListOf<Char>()
    private var errores = 0

    // --- PUNTUACIÓN ---
    private var puntuacionActual = 0

    // --- Variables Explicación/Audio ---
    private lateinit var contenedorFaseExplicacion: View
    private lateinit var btnPlayPause: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var btnJarraituExplicacion: Button
    private var mediaPlayer: MediaPlayer? = null
    private val handlerAudio = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ahorcado)
        inicializarVistas()
        iniciarJuego()
    }

    private fun inicializarVistas() {
        // --- CONFIGURACIÓN BOTÓN VOLVER AL MAPA ---
        btnVolverMapa = findViewById(R.id.btnVolverMapa)
        btnVolverMapa.visibility = View.VISIBLE // Aseguramos que se ve al principio
        btnVolverMapa.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            finish()
        }

        // NUEVO: Inicializar y animar mascota
        ivMascotaAnimada = findViewById(R.id.ivMascotaAnimada)
        animarMascota()

        // Fase Juego
        contenedorFaseJuego = findViewById(R.id.fase1_Juego)
        ivAhorcado = findViewById(R.id.ivAhorcado)
        tvPalabra = findViewById(R.id.tvPalabra)
        tvResultado = findViewById(R.id.tvResultado)
        llTeclado = findViewById(R.id.llTeclado)
        btnJarraituJuego = findViewById(R.id.btnJarraituJuego)

        // Configuración inicial del botón Jarraitu (DESACTIVADO - Color Gris)
        btnJarraituJuego.isEnabled = false
        val colorDesactivado = ContextCompat.getColor(this, R.color.boton_desactivado)
        btnJarraituJuego.backgroundTintList = ColorStateList.valueOf(colorDesactivado)

        btnJarraituJuego.setOnClickListener { mostrarFaseExplicacion() }

        // Fase Explicación
        contenedorFaseExplicacion = findViewById(R.id.fase_Explicacion)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        seekBar = findViewById(R.id.seekBarAudio)
        btnJarraituExplicacion = findViewById(R.id.btnJarraituExplicacion)

        btnJarraituExplicacion.setOnClickListener { finish() }

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

    private fun iniciarJuego() {
        errores = 0
        puntuacionActual = 0
        letrasAdivinadas.clear()
        palabraActual = palabras.random()

        contenedorFaseJuego.visibility = View.VISIBLE
        contenedorFaseExplicacion.visibility = View.GONE
        tvResultado.visibility = View.GONE

        // Reset imagen y botón
        ivAhorcado.setImageResource(R.drawable.ahorcado0)
        btnJarraituJuego.isEnabled = false
        val colorDesactivado = ContextCompat.getColor(this, R.color.boton_desactivado)
        btnJarraituJuego.backgroundTintList = ColorStateList.valueOf(colorDesactivado)

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
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
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
            // Acierto
            if (!letrasAdivinadas.contains(letra)) {
                letrasAdivinadas.add(letra)
                puntuacionActual += 10
            }
            boton.isEnabled = false
            // Color Acierto (Verde)
            boton.setBackgroundColor(ContextCompat.getColor(this, R.color.mi_acierto))
            actualizarTextoPantalla()
            verificarVictoria()
        } else {
            // Fallo
            errores++
            puntuacionActual -= 5
            if (puntuacionActual < 0) puntuacionActual = 0

            actualizarImagenAhorcado()
            if (errores >= 7) mostrarResultadoFinal(gano = false)
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
        if (gano) mostrarResultadoFinal(gano = true)
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

    private fun mostrarResultadoFinal(gano: Boolean) {
        desactivarTeclado()
        tvResultado.visibility = View.VISIBLE
        if (gano) {
            val bonusVictoria = 100
            val vidasRestantes = 7 - errores
            val bonusVidas = vidasRestantes * 20
            puntuacionActual += (bonusVictoria + bonusVidas)
            tvResultado.text = "OSO ONDO! IRABAZI DUZU!"
            tvResultado.setTextColor(ContextCompat.getColor(this, R.color.mi_acierto))
            ivAhorcado.setImageResource(R.drawable.leonfeliz)
        } else {
            tvResultado.text = "GALDU DUZU... HITZ: $palabraActual"
            tvResultado.setTextColor(ContextCompat.getColor(this, R.color.mi_error_texto))
            ivAhorcado.setImageResource(R.drawable.leontriste)
            tvPalabra.text = palabraActual.replace(" ", "\n")
        }
        guardarPuntuacionEnBD(puntuacionActual)

        btnJarraituJuego.isEnabled = true

        // --- CORRECCIÓN AQUÍ: Usamos el color 'ahorcado' (Morado) ---
        val colorActivo = ContextCompat.getColor(this, R.color.ahorcado)
        btnJarraituJuego.backgroundTintList = ColorStateList.valueOf(colorActivo)

        Toast.makeText(this, "Jokoa amaitu da. Sakatu Jarraitu.", Toast.LENGTH_SHORT).show()
    }

    private fun desactivarTeclado() {
        for (i in 0 until llTeclado.childCount) {
            val fila = llTeclado.getChildAt(i) as LinearLayout
            for (j in 0 until fila.childCount) {
                val boton = fila.getChildAt(j)
                boton.isEnabled = false
            }
        }
    }

    private fun guardarPuntuacionEnBD(puntos: Int) {
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombreAlumno = prefs.getString("nombre_alumno_actual", "Anonimo") ?: "Anonimo"
        val dbHelper = DatabaseHelper(this)
        dbHelper.guardarPuntuacion(nombreAlumno, "Ahorcado", puntos)
    }

    private fun mostrarFaseExplicacion() {
        contenedorFaseJuego.visibility = View.GONE
        contenedorFaseExplicacion.visibility = View.VISIBLE

        // OCULTAMOS EL BOTÓN DEL MAPA AL PASAR A LA EXPLICACIÓN
        btnVolverMapa.visibility = View.GONE

        configurarAudio()
    }

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

    private fun animarMascota() {
        try {
            ivMascotaAnimada.setImageResource(R.drawable.leonexplicacion)
            val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_bounce_in)

            bounceAnim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {}

                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    ivMascotaAnimada.clearAnimation()
                }

                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            })

            ivMascotaAnimada.startAnimation(bounceAnim)
        } catch (e: Exception) { }
    }

    override fun onDestroy() {
        super.onDestroy()
        handlerAudio.removeCallbacks(actualizarSeekBarRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}