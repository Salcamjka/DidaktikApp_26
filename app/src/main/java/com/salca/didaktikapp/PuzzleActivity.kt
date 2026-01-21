package com.salca.didaktikapp

import android.content.ClipData
import android.content.Context
import android.content.Intent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_puzzle)

        contenedorJuego = findViewById(R.id.contenedorJuego)
        layoutFinal = findViewById(R.id.layoutFinal)
        txtTituloPrincipal = findViewById(R.id.txtTituloPrincipal)

        // Botón Volver Mapa (arriba izquierda)
        btnVolverMapa = findViewById(R.id.btnVolverMapa)
        btnVolverMapa.setOnClickListener {
            if (isPlaying) pauseAudio()
            finish()
        }

        val gridPasado = findViewById<GridLayout>(R.id.gridPasado)
        val gridPresente = findViewById<GridLayout>(R.id.gridPresente)
        val gridPiezas = findViewById<GridLayout>(R.id.gridPiezas)
        btnJarraitu = findViewById(R.id.btnJarraitu)

        // ================================================================
        // ESTADO INICIAL: BOTÓN DESACTIVADO (Modo Juego)
        // ================================================================
        btnJarraitu.visibility = View.VISIBLE
        btnJarraitu.isEnabled = false // <--- DESACTIVADO: Debe completar los puzzles

        val colorDesactivado = ContextCompat.getColor(this, R.color.boton_desactivado)
        btnJarraitu.backgroundTintList = ColorStateList.valueOf(colorDesactivado)
        btnJarraitu.setTextColor(Color.WHITE)

        btnJarraitu.setOnClickListener {
            cambiarAPantallaFinal()
        }
        // ================================================================

        // Botón final de la explicación
        findViewById<Button>(R.id.btnFinalizarTotal)?.setOnClickListener {
            SyncHelper.subirInmediatamente(this)

            val intent = Intent(this, MapActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
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
                    view.visibility = View.INVISIBLE
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
                            Toast.makeText(this, "Hori ez doa hor!", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        if (!event.result) (event.localState as View).visibility = View.VISIBLE
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
                Toast.makeText(this, "Lehenaldia osatuta! (+250 pts)", Toast.LENGTH_SHORT).show()
                guardarPuntuacionEnBD(puntuacionTotal)
            }
        } else {
            aciertosOrainaldia++
            if (aciertosOrainaldia == PIEZAS_POR_PUZZLE && !completadoOrainaldia) {
                completadoOrainaldia = true
                puntuacionTotal += PUNTOS_POR_PUZZLE
                Toast.makeText(this, "Orainaldia osatuta! (+250 pts)", Toast.LENGTH_SHORT).show()
                guardarPuntuacionEnBD(puntuacionTotal)
            }
        }

        // --- SOLO SE ACTIVA CUANDO AMBOS PUZZLES ESTÁN COMPLETOS ---
        if (completadoLehenaldia && completadoOrainaldia) {
            Toast.makeText(this, "Puzzleak osatuta! Jarraitu dezakezu.", Toast.LENGTH_LONG).show()

            // AHORA SÍ ACTIVAMOS EL BOTÓN
            btnJarraitu.isEnabled = true

            // CAMBIAMOS AL COLOR 'PUZZLE' (Coral)
            val colorActivo = ContextCompat.getColor(this, R.color.puzzle)
            btnJarraitu.backgroundTintList = ColorStateList.valueOf(colorActivo)
            btnJarraitu.setTextColor(Color.BLACK)

            SyncHelper.subirInmediatamente(this)
        }
    }

    private fun cambiarAPantallaFinal() {
        // 1. Ocultamos el juego
        contenedorJuego.visibility = View.GONE

        // 2. Ocultamos el botón de volver al mapa
        btnVolverMapa.visibility = View.INVISIBLE

        // 3. Cambiamos el título
        txtTituloPrincipal.visibility = View.VISIBLE
        txtTituloPrincipal.text = "Bilboko Areatza"

        // 4. Mostramos la explicación
        layoutFinal.visibility = View.VISIBLE

        // 5. Scroll arriba
        val scrollView = findViewById<ScrollView>(R.id.scrollViewMain)
        scrollView.post { scrollView.fullScroll(View.FOCUS_UP) }

        setupAudioPlayer()
    }

    private fun guardarPuntuacionEnBD(puntos: Int) {
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombreAlumno = prefs.getString("nombre_alumno_actual", "Anonimo") ?: "Anonimo"
        val dbHelper = DatabaseHelper(this)
        dbHelper.guardarPuntuacion(nombreAlumno, "Puzzle", puntos)

        // Sincronizar inmediatamente
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