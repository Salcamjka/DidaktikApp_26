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
import com.bumptech.glide.Glide

/**
 * Actividad que implementa el juego de Puzle sobre "Bilboko Areatza" (El Arenal).
 *
 * Mecánica del juego:
 * * El alumno debe reconstruir dos imágenes (Pasado y Presente) arrastrando piezas sueltas.
 * * Utiliza la API de `Drag and Drop` de Android.
 * * Al completar ambos puzles, se desbloquea una explicación histórica con audio.
 *
 * @author Nizam
 * @version 1.0
 */
class PuzzleActivity : AppCompatActivity() {

    // --- Variables de Estado del Juego ---
    private var aciertosLehenaldia = 0 // Aciertos en el puzle del Pasado
    private var aciertosOrainaldia = 0 // Aciertos en el puzle del Presente
    private var completadoLehenaldia = false
    private var completadoOrainaldia = false

    // Constantes del tablero
    private val PIEZAS_POR_PUZZLE = 12 // Grid de 4x3
    private val PUNTOS_POR_PUZZLE = 250
    private var puntuacionTotal = 0

    // Tamaño fijo de las piezas para asegurar que encajen en el GridLayout
    private val PIEZA_ANCHO = 150
    private val PIEZA_ALTO = 110

    // --- Variables de Audio ---
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private lateinit var runnable: Runnable
    private var handler = Handler(Looper.getMainLooper())
    private lateinit var seekBarAudio: SeekBar
    private lateinit var btnAudio: ImageButton

    // --- Variables de UI (Interfaz) ---
    private lateinit var contenedorJuego: LinearLayout
    private lateinit var layoutFinal: LinearLayout
    private lateinit var btnJarraitu: Button
    private lateinit var txtTituloPrincipal: TextView
    private lateinit var btnVolverMapa: ImageButton

    // Textos informativos
    private lateinit var tvInstruccionArrastrar: TextView
    private lateinit var tvMensajeVictoria: TextView

    // Imagen final (GIF)
    private lateinit var ivGifResultado: ImageView

    /**
     * Método de creación de la actividad.
     *
     * Inicializa los tableros, carga las imágenes, configura el arrastre de piezas (Drag&Drop)
     * y aplica los ajustes de accesibilidad.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_puzzle)

        // --- 1. Inicialización de Vistas ---
        contenedorJuego = findViewById(R.id.contenedorJuego)
        layoutFinal = findViewById(R.id.layoutFinal)
        txtTituloPrincipal = findViewById(R.id.txtTituloPrincipal)

        tvInstruccionArrastrar = findViewById(R.id.tvInstruccionArrastrar)
        tvMensajeVictoria = findViewById(R.id.tvMensajeVictoria)
        ivGifResultado = findViewById(R.id.ivGifResultado)

        // Botón salir
        btnVolverMapa = findViewById(R.id.btnVolverMapa)
        btnVolverMapa.setOnClickListener {
            if (isPlaying) pauseAudio()
            finish()
        }

        // Grids (Mallas) donde se soltarán las piezas
        val gridPasado = findViewById<GridLayout>(R.id.gridPasado)
        val gridPresente = findViewById<GridLayout>(R.id.gridPresente)
        // Grid donde aparecen las piezas desordenadas
        val gridPiezas = findViewById<GridLayout>(R.id.gridPiezas)

        btnJarraitu = findViewById(R.id.btnJarraitu)
        val btnFinalizarTotal = findViewById<Button>(R.id.btnFinalizarTotal)

        // --- 2. Accesibilidad (Texto Grande) ---
        val sharedPref = getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
        val usarTextoGrande = sharedPref.getBoolean("MODO_TEXTO_GRANDE", false)

        if (usarTextoGrande) {
            txtTituloPrincipal.textSize = 32f
            findViewById<TextView>(R.id.tvLabelPasado)?.textSize = 16f
            findViewById<TextView>(R.id.tvLabelPresente)?.textSize = 16f
            tvInstruccionArrastrar.textSize = 22f
            tvMensajeVictoria.textSize = 26f
            findViewById<TextView>(R.id.tvExplicacionFinal)?.textSize = 22f
            btnJarraitu.textSize = 22f
            btnFinalizarTotal.textSize = 22f
        }

        // Configuración inicial del botón continuar (desactivado)
        btnJarraitu.isEnabled = false
        val colorInactivo = ContextCompat.getColor(this, R.color.boton_desactivado)
        btnJarraitu.backgroundTintList = ColorStateList.valueOf(colorInactivo)

        // Al pulsar continuar, mostramos la explicación histórica
        btnJarraitu.setOnClickListener { cambiarAPantallaFinal() }

        btnFinalizarTotal.setOnClickListener {
            SyncHelper.subirInmediatamente(this)
            finish()
        }

        // --- 3. Carga y Preparación de Piezas ---
        // Obtenemos los IDs de los recursos drawables dinámicamente
        val imagenesPasado = Array(PIEZAS_POR_PUZZLE) { i -> resources.getIdentifier("pasado$i", "drawable", packageName) }
        val imagenesPresente = Array(PIEZAS_POR_PUZZLE) { i -> resources.getIdentifier("presente$i", "drawable", packageName) }

        // Creamos los huecos vacíos en los tableros de destino
        crearTableroVacio(gridPasado, "lehenaldia")
        crearTableroVacio(gridPresente, "orainaldia")

        // Creamos la lista de piezas mezcladas
        val todasLasPiezas = mutableListOf<PiezaPuzzle>()
        for (i in 0 until PIEZAS_POR_PUZZLE) todasLasPiezas.add(PiezaPuzzle(i, "lehenaldia", imagenesPasado[i]))
        for (i in 0 until PIEZAS_POR_PUZZLE) todasLasPiezas.add(PiezaPuzzle(i, "orainaldia", imagenesPresente[i]))
        todasLasPiezas.shuffle() // ¡Importante! Barajar las piezas

        // --- 4. Generación de las Piezas Arrastrables ---
        for (pieza in todasLasPiezas) {
            val img = ImageView(this)
            img.setImageResource(pieza.imagenID)
            img.tag = pieza // Guardamos el objeto pieza dentro de la vista para recuperarlo luego
            img.scaleType = ImageView.ScaleType.FIT_XY

            val params = GridLayout.LayoutParams()
            params.width = PIEZA_ANCHO
            params.height = PIEZA_ALTO
            params.setMargins(6, 6, 6, 6)
            img.layoutParams = params

            // Listener para iniciar el arrastre (Drag)
            img.setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val data = ClipData.newPlainText("mota", pieza.tipo)
                    val shadow = View.DragShadowBuilder(view)
                    // Iniciamos el drag
                    view.startDragAndDrop(data, shadow, view, 0)
                    true
                } else {
                    false
                }
            }
            gridPiezas.addView(img)
        }
    }

    /**
     * Genera los huecos vacíos (grisáceos) en el tablero de destino y configura sus listeners
     * para aceptar las piezas que se sueltan (Drop).
     *
     * @param grid El GridLayout donde se añadirán los huecos.
     * @param tipoTablero Identificador ("lehenaldia" o "orainaldia") para validar que la pieza corresponde a este tablero.
     */
    private fun crearTableroVacio(grid: GridLayout, tipoTablero: String) {
        for (i in 0 until PIEZAS_POR_PUZZLE) {
            val hueco = ImageView(this)
            hueco.setBackgroundColor(Color.LTGRAY) // Fondo gris para indicar hueco vacío
            hueco.scaleType = ImageView.ScaleType.FIT_XY

            val params = GridLayout.LayoutParams()
            params.width = PIEZA_ANCHO
            params.height = PIEZA_ALTO
            params.setMargins(2, 2, 2, 2)

            hueco.layoutParams = params
            hueco.tag = i // El tag del hueco es su posición (0..11)

            // Listener para recibir piezas (Drop)
            hueco.setOnDragListener { view, event ->
                val huecoDestino = view as ImageView
                when (event.action) {
                    DragEvent.ACTION_DROP -> {
                        // Recuperamos los datos de la pieza que se está soltando
                        val piezaArrastrada = event.localState as View
                        val datosPieza = piezaArrastrada.tag as PiezaPuzzle
                        val idEsperado = huecoDestino.tag as Int

                        // VALIDACIÓN: ¿La pieza es de este tablero? Y ¿Es la posición correcta?
                        if (datosPieza.tipo == tipoTablero && datosPieza.id == idEsperado) {
                            // CORRECTO: Fijamos la imagen en el hueco
                            huecoDestino.setImageResource(datosPieza.imagenID)
                            huecoDestino.setBackgroundColor(Color.TRANSPARENT)
                            huecoDestino.setOnDragListener(null) // Ya no aceptamos más drops aquí

                            // Eliminamos la pieza del grid de origen
                            (piezaArrastrada.parent as GridLayout).removeView(piezaArrastrada)

                            verificarProgreso(datosPieza.tipo)
                        } else {
                            // INCORRECTO: Hacemos visible la pieza de nuevo en su origen
                            piezaArrastrada.visibility = View.VISIBLE
                        }
                        true
                    }
                    DragEvent.ACTION_DRAG_STARTED -> {
                        // Al empezar a arrastrar, ocultamos la pieza original
                        val piezaArrastrada = event.localState as? View
                        piezaArrastrada?.visibility = View.INVISIBLE
                        true
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        // Si el drop falló o se canceló, volvemos a mostrar la pieza
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

    /**
     * Comprueba si se ha completado alguno de los dos puzles y gestiona la victoria final.
     *
     * @param tipo El tipo de puzle donde se ha colocado la pieza ("lehenaldia" o "orainaldia").
     */
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

        // SI AMBOS PUZLES ESTÁN COMPLETOS
        if (completadoLehenaldia && completadoOrainaldia) {
            SyncHelper.subirInmediatamente(this)
            tvInstruccionArrastrar.visibility = View.GONE

            // Guardamos el estado completado
            val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
            val nombreUsuario = prefs.getString("nombre_alumno_actual", "") ?: ""
            prefs.edit().putBoolean("completado_puzzle_$nombreUsuario", true).apply()

            // Feedback visual
            tvMensajeVictoria.visibility = View.VISIBLE
            ivGifResultado.visibility = View.VISIBLE
            try {
                Glide.with(this).asGif().load(R.drawable.leonfeliz).into(ivGifResultado)
            } catch (e: Exception) {
                ivGifResultado.setImageResource(R.drawable.leonfeliz)
            }

            // Habilitamos botón para ir a la explicación
            btnJarraitu.isEnabled = true
            val colorActivo = ContextCompat.getColor(this, R.color.puzzle)
            btnJarraitu.backgroundTintList = ColorStateList.valueOf(colorActivo)
            btnJarraitu.setTextColor(Color.WHITE)
        }
    }

    /**
     * Oculta el juego y muestra la pantalla final con la explicación histórica y el audio.
     */
    private fun cambiarAPantallaFinal() {
        contenedorJuego.visibility = View.GONE
        btnVolverMapa.visibility = View.INVISIBLE

        // Ajustamos título
        txtTituloPrincipal.visibility = View.VISIBLE
        txtTituloPrincipal.text = "Bilboko Areatza"

        layoutFinal.visibility = View.VISIBLE

        // Scroll automático hacia arriba
        val scrollView = findViewById<ScrollView>(R.id.scrollViewMain)
        scrollView.post { scrollView.fullScroll(View.FOCUS_UP) }

        setupAudioPlayer()
    }

    /** Guarda la puntuación en la base de datos local. */
    private fun guardarPuntuacionEnBD(puntos: Int) {
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombreAlumno = prefs.getString("nombre_alumno_actual", "Anonimo") ?: "Anonimo"
        val dbHelper = DatabaseHelper(this)
        dbHelper.guardarPuntuacion(nombreAlumno, "Puzzle", puntos)
        SyncHelper.subirInmediatamente(this)
    }

    // ================================================================
    // MÉTODOS DE AUDIO
    // ================================================================

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
            // Control manual de la barra
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

    /** Clase de datos interna para manejar la información de cada pieza. */
    data class PiezaPuzzle(val id: Int, val tipo: String, val imagenID: Int)
}