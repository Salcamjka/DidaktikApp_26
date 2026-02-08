package com.salca.didaktikapp

import android.content.ClipData
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide // Importante

class PuzzleActivity : AppCompatActivity() {

    // Contadores
    private var aciertosLehenaldia = 0
    private var aciertosOrainaldia = 0
    private var completadoLehenaldia = false
    private var completadoOrainaldia = false

    // 12 piezas (4x3)
    private val PIEZAS_POR_PUZZLE = 12
    private val PUNTOS_POR_PUZZLE = 250
    private var puntuacionTotal = 0

    // Audio
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private lateinit var runnable: Runnable
    private var handler = Handler(Looper.getMainLooper())
    private lateinit var seekBarAudio: SeekBar
    private lateinit var btnAudio: ImageButton

    // Contenedores
    private lateinit var contenedorJuego: LinearLayout
    private lateinit var layoutFinal: LinearLayout
    private lateinit var btnJarraitu: Button
    private lateinit var txtTituloPrincipal: TextView
    private lateinit var btnVolverMapa: ImageButton

    // Referencias a textos
    private lateinit var tvInstruccionArrastrar: TextView
    private lateinit var tvMensajeVictoria: TextView

    // NUEVO: Referencia al GIF
    private lateinit var ivGifResultado: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_puzzle)

        contenedorJuego = findViewById(R.id.contenedorJuego)
        layoutFinal = findViewById(R.id.layoutFinal)
        txtTituloPrincipal = findViewById(R.id.txtTituloPrincipal)

        tvInstruccionArrastrar = findViewById(R.id.tvInstruccionArrastrar)
        tvMensajeVictoria = findViewById(R.id.tvMensajeVictoria)

        // Inicializamos el ImageView del GIF
        ivGifResultado = findViewById(R.id.ivGifResultado)

        // Botón Volver Mapa
        btnVolverMapa = findViewById(R.id.btnVolverMapa)
        btnVolverMapa.setOnClickListener {
            if (isPlaying) pauseAudio()
            finish()
        }

        val gridPasado = findViewById<GridLayout>(R.id.gridPasado)
        val gridPresente = findViewById<GridLayout>(R.id.gridPresente)
        val gridPiezas = findViewById<GridLayout>(R.id.gridPiezas)
        btnJarraitu = findViewById(R.id.btnJarraitu)
        val btnFinalizarTotal = findViewById<Button>(R.id.btnFinalizarTotal)

        // ---------------------------------------------------------------
        // ACCESIBILIDAD: LETRA GRANDE
        // ---------------------------------------------------------------
        val sharedPref = getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
        val usarTextoGrande = sharedPref.getBoolean("MODO_TEXTO_GRANDE", false)

        if (usarTextoGrande) {
            txtTituloPrincipal.textSize = 34f
            findViewById<TextView>(R.id.tvLabelPasado)?.textSize = 18f
            findViewById<TextView>(R.id.tvLabelPresente)?.textSize = 18f
            tvInstruccionArrastrar.textSize = 24f
            tvMensajeVictoria.textSize = 30f
            findViewById<TextView>(R.id.tvExplicacionFinal)?.textSize = 24f
            btnJarraitu.textSize = 22f
            btnFinalizarTotal.textSize = 22f
        }
        // ---------------------------------------------------------------

        // --- CAMBIO: BOTÓN DESACTIVADO AL PRINCIPIO ---
        btnJarraitu.visibility = View.VISIBLE
        btnJarraitu.isEnabled = false // No se puede pulsar
        val colorInactivo = ContextCompat.getColor(this, R.color.boton_desactivado)
        btnJarraitu.backgroundTintList = ColorStateList.valueOf(colorInactivo)
        // ---------------------------------------------

        btnJarraitu.setOnClickListener {
            cambiarAPantallaFinal()
        }

        btnFinalizarTotal.setOnClickListener {
            SyncHelper.subirInmediatamente(this)
            finish()
        }

        // Carga de imágenes
        val imagenesPasado = Array(PIEZAS_POR_PUZZLE) { i -> resources.getIdentifier("pasado$i", "drawable", packageName) }
        val imagenesPresente = Array(PIEZAS_POR_PUZZLE) { i -> resources.getIdentifier("presente$i", "drawable", packageName) }

        crearTableroVacio(gridPasado, "lehenaldia")
        crearTableroVacio(gridPresente, "orainaldia")

        val todasLasPiezas = mutableListOf<PiezaPuzzle>()
        for (i in 0 until PIEZAS_POR_PUZZLE) todasLasPiezas.add(PiezaPuzzle(i, "lehenaldia", imagenesPasado[i]))
        for (i in 0 until PIEZAS_POR_PUZZLE) todasLasPiezas.add(PiezaPuzzle(i, "orainaldia", imagenesPresente[i]))
        todasLasPiezas.shuffle()

        // CONFIGURACIÓN DE LAS PIEZAS PARA ARRASTRAR
        for (pieza in todasLasPiezas) {
            val img = ImageView(this)
            img.setImageResource(pieza.imagenID)
            img.tag = pieza
            img.scaleType = ImageView.ScaleType.FIT_XY

            val params = GridLayout.LayoutParams()
            params.width = 180
            params.height = 130
            params.setMargins(5, 5, 5, 5)
            img.layoutParams = params

            img.setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val data = ClipData.newPlainText("mota", pieza.tipo)
                    val shadow = View.DragShadowBuilder(view)
                    view.startDragAndDrop(data, shadow, view, 0)
                    true
                } else {
                    false
                }
            }
            gridPiezas.addView(img)
        }
    }

    private fun crearTableroVacio(grid: GridLayout, tipoTablero: String) {
        for (i in 0 until PIEZAS_POR_PUZZLE) {
            val hueco = ImageView(this)
            hueco.setBackgroundColor(Color.LTGRAY)
            hueco.scaleType = ImageView.ScaleType.FIT_XY
            val params = GridLayout.LayoutParams()
            params.width = 180
            params.height = 130
            params.setMargins(2, 2, 2, 2)
            hueco.layoutParams = params
            hueco.tag = i

            hueco.setOnDragListener { view, event ->
                val huecoDestino = view as ImageView
                when (event.action) {
                    DragEvent.ACTION_DROP -> {
                        val piezaArrastrada = event.localState as View
                        val datosPieza = piezaArrastrada.tag as PiezaPuzzle
                        val idEsperado = huecoDestino.tag as Int

                        if (datosPieza.tipo == tipoTablero && datosPieza.id == idEsperado) {
                            huecoDestino.setImageResource(datosPieza.imagenID)
                            huecoDestino.setBackgroundColor(Color.TRANSPARENT)
                            huecoDestino.setOnDragListener(null)
                            (piezaArrastrada.parent as GridLayout).removeView(piezaArrastrada)
                            verificarProgreso(datosPieza.tipo)
                        } else {
                            piezaArrastrada.visibility = View.VISIBLE
                        }
                        true
                    }
                    DragEvent.ACTION_DRAG_STARTED -> {
                        val piezaArrastrada = event.localState as? View
                        piezaArrastrada?.visibility = View.INVISIBLE
                        true
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        if (!event.result) {
                            val piezaArrastrada = event.localState as? View
                            piezaArrastrada?.visibility = View.VISIBLE
                        }
                        true
                    }
                    else -> true
                }
            }
            grid.addView(hueco)
        }
    }

    private fun verificarProgreso(tipo: String) {
        if (tipo == "lehenaldia") {
            aciertosLehenaldia++
            if (aciertosLehenaldia == PIEZAS_POR_PUZZLE && !completadoLehenaldia) {
                completadoLehenaldia = true
                puntuacionTotal += PUNTOS_POR_PUZZLE
                guardarPuntuacionEnBD(puntuacionTotal)
            }
        } else {
            aciertosOrainaldia++
            if (aciertosOrainaldia == PIEZAS_POR_PUZZLE && !completadoOrainaldia) {
                completadoOrainaldia = true
                puntuacionTotal += PUNTOS_POR_PUZZLE
                guardarPuntuacionEnBD(puntuacionTotal)
            }
        }

        // --- SI AMBOS PUZZLES ESTÁN COMPLETOS ---
        if (completadoLehenaldia && completadoOrainaldia) {
            SyncHelper.subirInmediatamente(this)
            tvInstruccionArrastrar.visibility = View.GONE
            // ========================================
            // ✅ NUEVO: MARCAR ACTIVIDAD COMO COMPLETADA PARA ESTE USUARIO
            // ========================================
            val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
            val nombreUsuario = prefs.getString("nombre_alumno_actual", "") ?: ""
            prefs.edit().putBoolean("completado_puzzle_$nombreUsuario", true).apply()
            // ========================================
            tvMensajeVictoria.visibility = View.VISIBLE

            // MOSTRAR GIF
            ivGifResultado.visibility = View.VISIBLE
            Glide.with(this).asGif().load(R.drawable.leonfeliz).into(ivGifResultado)

            // ACTIVAMOS EL BOTÓN
            btnJarraitu.isEnabled = true
            val colorActivo = ContextCompat.getColor(this, R.color.puzzle)
            btnJarraitu.backgroundTintList = ColorStateList.valueOf(colorActivo)
            btnJarraitu.setTextColor(Color.BLACK)
        }
    }

    private fun cambiarAPantallaFinal() {
        contenedorJuego.visibility = View.GONE
        btnVolverMapa.visibility = View.INVISIBLE
        txtTituloPrincipal.visibility = View.VISIBLE
        txtTituloPrincipal.text = "Bilboko Areatza"
        layoutFinal.visibility = View.VISIBLE
        val scrollView = findViewById<ScrollView>(R.id.scrollViewMain)
        scrollView.post { scrollView.fullScroll(View.FOCUS_UP) }
        setupAudioPlayer()
    }

    private fun guardarPuntuacionEnBD(puntos: Int) {
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombreAlumno = prefs.getString("nombre_alumno_actual", "Anonimo") ?: "Anonimo"
        val dbHelper = DatabaseHelper(this)
        dbHelper.guardarPuntuacion(nombreAlumno, "Puzzle", puntos)
        SyncHelper.subirInmediatamente(this)
    }

    private fun setupAudioPlayer() {
        btnAudio = findViewById(R.id.btnAudio)
        seekBarAudio = findViewById(R.id.seekBarAudio)
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.jarduera_5)
            mediaPlayer?.setOnPreparedListener { mp -> seekBarAudio.max = mp.duration }
            mediaPlayer?.setOnCompletionListener {
                btnAudio.setImageResource(android.R.drawable.ic_media_play)
                seekBarAudio.progress = 0
                isPlaying = false
                if (::runnable.isInitialized) handler.removeCallbacks(runnable)
            }
            seekBarAudio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) mediaPlayer?.seekTo(progress)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            btnAudio.setOnClickListener { if (isPlaying) pauseAudio() else playAudio() }
        } catch (e: Exception) { }
    }

    private fun playAudio() {
        mediaPlayer?.start()
        isPlaying = true
        btnAudio.setImageResource(android.R.drawable.ic_media_pause)
        updateSeekBar()
    }

    private fun pauseAudio() {
        mediaPlayer?.pause()
        isPlaying = false
        btnAudio.setImageResource(android.R.drawable.ic_media_play)
        if (::runnable.isInitialized) handler.removeCallbacks(runnable)
    }

    private fun updateSeekBar() {
        runnable = Runnable {
            seekBarAudio.progress = mediaPlayer?.currentPosition ?: 0
            handler.postDelayed(runnable, 500)
        }
        handler.postDelayed(runnable, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::runnable.isInitialized) handler.removeCallbacks(runnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }

    data class PiezaPuzzle(val id: Int, val tipo: String, val imagenID: Int)
}