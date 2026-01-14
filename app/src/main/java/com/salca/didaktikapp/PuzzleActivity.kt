package com.salca.didaktikapp

import android.content.ClipData
import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class PuzzleActivity : AppCompatActivity() {

    // Contadores separados para cada puzzle
    private var aciertosLehenaldia = 0
    private var aciertosOrainaldia = 0

    // Banderas para saber si ya hemos sumado los puntos de cada uno
    private var completadoLehenaldia = false
    private var completadoOrainaldia = false

    private val PIEZAS_POR_PUZZLE = 24
    private val PUNTOS_POR_PUZZLE = 250 // 250 por cada uno (Total 500)

    private var puntuacionTotal = 0
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_puzzle)

        val gridPasado = findViewById<GridLayout>(R.id.gridPasado)
        val gridPresente = findViewById<GridLayout>(R.id.gridPresente)
        val gridPiezas = findViewById<GridLayout>(R.id.gridPiezas)

        val btnJarraitu = findViewById<Button>(R.id.btnJarraitu)
        btnJarraitu.visibility = View.GONE

        btnJarraitu.setOnClickListener { mostrarSeccionFinal() }

        // Carga de imágenes
        val imagenesPasado = Array(24) { i -> resources.getIdentifier("pasado$i", "drawable", packageName) }
        val imagenesPresente = Array(24) { i -> resources.getIdentifier("presente$i", "drawable", packageName) }

        // Creamos los tableros vacíos
        crearTableroVacio(gridPasado, "lehenaldia")
        crearTableroVacio(gridPresente, "orainaldia")

        // Creamos y mezclamos las piezas
        val todasLasPiezas = mutableListOf<PiezaPuzzle>()
        for (i in 0 until 24) todasLasPiezas.add(PiezaPuzzle(i, "lehenaldia", imagenesPasado[i]))
        for (i in 0 until 24) todasLasPiezas.add(PiezaPuzzle(i, "orainaldia", imagenesPresente[i]))
        todasLasPiezas.shuffle()

        // Añadimos las piezas al grid inferior
        for (pieza in todasLasPiezas) {
            val img = ImageView(this)
            img.setImageResource(pieza.imagenID)
            img.tag = pieza

            // Que la pieza ocupe todo su espacio disponible
            img.scaleType = ImageView.ScaleType.FIT_XY

            val params = GridLayout.LayoutParams()
            params.width = 130
            params.height = 90
            // Margen para las piezas de abajo (para que sea fácil cogerlas)
            params.setMargins(5, 5, 5, 5)
            img.layoutParams = params

            // USO DE onTouchListener (Agarre instantáneo)
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
        for (i in 0 until 24) {
            val hueco = ImageView(this)
            hueco.setBackgroundColor(Color.LTGRAY)

            // 1. Esto asegura que la imagen rellene el hueco
            hueco.scaleType = ImageView.ScaleType.FIT_XY

            val params = GridLayout.LayoutParams()
            params.width = 130
            params.height = 90

            // 2. RESTAURAMOS LOS MÁRGENES (Para mantener la cuadrícula visible)
            params.setMargins(2, 2, 2, 2)
            // ---------------------------------------------------------------

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
                            // Colocar pieza
                            huecoDestino.setImageResource(datosPieza.imagenID)
                            huecoDestino.setBackgroundColor(Color.TRANSPARENT)
                            huecoDestino.setOnDragListener(null)
                            (piezaArrastrada.parent as GridLayout).removeView(piezaArrastrada)

                            // Sumar acierto
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

        if (completadoLehenaldia && completadoOrainaldia) {
            findViewById<Button>(R.id.btnJarraitu).visibility = View.VISIBLE
            mostrarSeccionFinal()
        }
    }

    private fun guardarPuntuacionEnBD(puntos: Int) {
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombreAlumno = prefs.getString("nombre_alumno_actual", "Anonimo") ?: "Anonimo"
        val dbHelper = DatabaseHelper(this)

        dbHelper.guardarPuntuacion(nombreAlumno, "Puzzle", puntos)
    }

    private fun mostrarSeccionFinal() {
        val layoutFinal = findViewById<LinearLayout>(R.id.layoutFinal)
        layoutFinal.visibility = View.VISIBLE

        val scrollView = findViewById<ScrollView>(R.id.scrollViewMain)
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }

        // AUDIO SIMPLE (SIN CAMBIOS)
        val btnAudio = findViewById<ImageButton>(R.id.btnAudio)
        btnAudio.setOnClickListener {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.jarduera_5)
                mediaPlayer?.setOnCompletionListener { btnAudio.setImageResource(android.R.drawable.ic_media_play) }
            }
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                btnAudio.setImageResource(android.R.drawable.ic_media_play)
            } else {
                mediaPlayer?.start()
                btnAudio.setImageResource(android.R.drawable.ic_media_pause)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }

    data class PiezaPuzzle(val id: Int, val tipo: String, val imagenID: Int)
}