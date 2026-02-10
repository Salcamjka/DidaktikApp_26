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

class AhorcadoActivity : AppCompatActivity() {

    private lateinit var contenedorFaseJuego: View
    private lateinit var ivAhorcado: ImageView
    private lateinit var tvPalabra: TextView
    private lateinit var tvResultado: TextView
    private lateinit var llTeclado: LinearLayout
    private lateinit var btnJarraituJuego: Button

    private lateinit var ivGifResultado: ImageView

    private lateinit var btnVolverMapa: ImageButton

    private val palabras = listOf("ATHLETIC-EN ARMARRIA")
    private var palabraActual = ""
    private var letrasAdivinadas = mutableListOf<Char>()
    private var errores = 0
    private var puntuacionActual = 0

    private lateinit var contenedorFaseExplicacion: View
    private lateinit var btnPlayPause: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var btnJarraituExplicacion: Button
    private var mediaPlayer: MediaPlayer? = null
    private val handlerAudio = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } catch (e: Exception) {}

        setContentView(R.layout.activity_ahorcado)

        inicializarVistas()

        val sharedPref = getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
        val usarTextoGrande = sharedPref.getBoolean("MODO_TEXTO_GRANDE", false)

        if (usarTextoGrande) {
            findViewById<TextView>(R.id.tvTituloCabecera)?.textSize = 34f
            tvResultado.textSize = 30f
            findViewById<TextView>(R.id.tvTextoExplicativo)?.textSize = 24f
            btnJarraituJuego.textSize = 22f
            btnJarraituExplicacion.textSize = 22f
        }

        iniciarJuego()
    }

    private fun inicializarVistas() {
        btnVolverMapa = findViewById(R.id.btnVolverMapa)
        btnVolverMapa.setOnClickListener {
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

        btnJarraituJuego.isEnabled = false
        val colorDesactivado = ContextCompat.getColor(this, R.color.boton_desactivado)
        btnJarraituJuego.backgroundTintList = ColorStateList.valueOf(colorDesactivado)

        btnJarraituJuego.setOnClickListener { mostrarFaseExplicacion() }

        contenedorFaseExplicacion = findViewById(R.id.fase_Explicacion)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        seekBar = findViewById(R.id.seekBarAudio)
        btnJarraituExplicacion = findViewById(R.id.btnJarraituExplicacion)

        btnJarraituExplicacion.setOnClickListener {
            SyncHelper.subirInmediatamente(this)
            finish()
        }

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
        ivGifResultado.visibility = View.GONE

        llTeclado.visibility = View.VISIBLE

        btnVolverMapa.visibility = View.VISIBLE

        ivAhorcado.setImageResource(R.drawable.ahorcado0)
        btnJarraituJuego.isEnabled = false
        val colorDesactivado = ContextCompat.getColor(this, R.color.boton_desactivado)
        btnJarraituJuego.backgroundTintList = ColorStateList.valueOf(colorDesactivado)

        crearTeclado()
        actualizarTextoPantalla()
    }

    private fun mostrarFaseExplicacion() {
        contenedorFaseJuego.visibility = View.GONE
        contenedorFaseExplicacion.visibility = View.VISIBLE
        btnVolverMapa.visibility = View.GONE
        configurarAudio()
    }

    private fun crearTeclado() {
        llTeclado.removeAllViews()
        var contador = 0
        var filaActual: LinearLayout? = null

        val marginPx = (2 * resources.displayMetrics.density).toInt()

        for (letra in 'A'..'Z') {
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

            val boton = Button(this)
            boton.text = letra.toString()
            boton.textSize = 14f

            boton.setPadding(0,0,0,0)
            boton.minWidth = 0
            boton.minimumWidth = 0
            boton.minHeight = 0
            boton.minimumHeight = 0

            val paramsBtn = LinearLayout.LayoutParams(
                0,
                120,
                1.0f
            )
            paramsBtn.setMargins(marginPx, 0, marginPx, 0)

            boton.layoutParams = paramsBtn
            boton.setOnClickListener { procesarLetra(letra, boton) }
            filaActual?.addView(boton)
            contador++
        }

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

    private fun procesarLetra(letra: Char, boton: Button) {
        if (palabraActual.contains(letra)) {
            if (!letrasAdivinadas.contains(letra)) {
                letrasAdivinadas.add(letra)
                puntuacionActual += 10
            }
            boton.isEnabled = false
            boton.setBackgroundColor(ContextCompat.getColor(this, R.color.mi_acierto))
            actualizarTextoPantalla()
            verificarVictoria()
        } else {
            errores++
            puntuacionActual -= 5
            if (puntuacionActual < 0) puntuacionActual = 0

            boton.isEnabled = false
            boton.setBackgroundColor(android.graphics.Color.GRAY)

            actualizarImagenAhorcado()
            if (errores >= 7) mostrarResultadoFinal(gano = false)
        }
    }

    private fun actualizarTextoPantalla() {
        val sb = StringBuilder()
        for (c in palabraActual) {
            if (c == ' ') sb.append("\n") else {
                if (letrasAdivinadas.contains(c) || c == '-') sb.append("$c ") else sb.append("_ ")
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
        llTeclado.visibility = View.GONE

        tvResultado.visibility = View.VISIBLE

        val gifResId: Int

        if (gano) {
            val bonusVictoria = 120
            val vidasRestantes = 7 - errores
            val bonusVidas = vidasRestantes * 40
            puntuacionActual += (bonusVictoria + bonusVidas)

            tvResultado.text = "OSO ONDO! IRABAZI DUZU!"
            tvResultado.setTextColor(ContextCompat.getColor(this, R.color.mi_acierto))
            ivAhorcado.setImageResource(R.drawable.escudo)

            gifResId = R.drawable.leonfeliz
            val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
            val nombreUsuario = prefs.getString("nombre_alumno_actual", "") ?: ""
            prefs.edit().putBoolean("completado_ahorcado_$nombreUsuario", true).apply()

        } else {
            tvResultado.text = "GALDU DUZU... HITZA: $palabraActual"
            tvResultado.setTextColor(ContextCompat.getColor(this, R.color.mi_error_texto))
            ivAhorcado.setImageResource(R.drawable.escudo)

            gifResId = R.drawable.leontriste
        }

        ivGifResultado.visibility = View.VISIBLE
        try {
            Glide.with(this).asGif().load(gifResId).into(ivGifResultado)
        } catch (e: Exception) {
            ivGifResultado.setImageResource(gifResId)
        }

        guardarPuntuacionEnBD(puntuacionActual)
        btnJarraituJuego.isEnabled = true
        val colorActivo = ContextCompat.getColor(this, R.color.ahorcado)
        btnJarraituJuego.backgroundTintList = ColorStateList.valueOf(colorActivo)

        btnJarraituJuego.setTextColor(android.graphics.Color.WHITE)
    }

    private fun guardarPuntuacionEnBD(puntos: Int) {
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombreAlumno = prefs.getString("nombre_alumno_actual", "Anonimo") ?: "Anonimo"
        val dbHelper = DatabaseHelper(this)
        dbHelper.guardarPuntuacion(nombreAlumno, "Ahorcado", puntos)
        SyncHelper.subirInmediatamente(this)
    }

    private fun configurarAudio() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.jarduera_1)
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