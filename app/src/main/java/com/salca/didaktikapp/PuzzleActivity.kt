package com.salca.didaktikapp

import android.content.ClipData
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.view.DragEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class PuzzleActivity : AppCompatActivity() {

    private var piezasAcertadas = 0
    private val TOTAL_PIEZAS = 48
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_puzzle)

        val gridPasado = findViewById<GridLayout>(R.id.gridPasado)
        val gridPresente = findViewById<GridLayout>(R.id.gridPresente)
        val gridPiezas = findViewById<GridLayout>(R.id.gridPiezas)

        // 1. Ocultar el botón al inicio
        val btnJarraitu = findViewById<Button>(R.id.btnJarraitu)
        btnJarraitu.visibility = View.GONE

        btnJarraitu.setOnClickListener {
            mostrarSeccionFinal()
        }

        // Carga de imágenes dinámica
        val imagenesPasado = Array(24) { i -> resources.getIdentifier("pasado$i", "drawable", packageName) }
        val imagenesPresente = Array(24) { i -> resources.getIdentifier("presente$i", "drawable", packageName) }

        crearTableroVacio(gridPasado, "lehenaldia")
        crearTableroVacio(gridPresente, "orainaldia")

        val todasLasPiezas = mutableListOf<PiezaPuzzle>()
        for (i in 0 until 24) todasLasPiezas.add(PiezaPuzzle(i, "lehenaldia", imagenesPasado[i]))
        for (i in 0 until 24) todasLasPiezas.add(PiezaPuzzle(i, "orainaldia", imagenesPresente[i]))
        todasLasPiezas.shuffle()

        for (pieza in todasLasPiezas) {
            val img = ImageView(this)
            img.setImageResource(pieza.imagenID)
            img.tag = pieza

            val params = GridLayout.LayoutParams()
            params.width = 130
            params.height = 90
            params.setMargins(5, 5, 5, 5)
            img.layoutParams = params

            img.setOnLongClickListener { view ->
                val data = ClipData.newPlainText("mota", pieza.tipo)
                val shadow = View.DragShadowBuilder(view)
                view.startDragAndDrop(data, shadow, view, 0)
                view.visibility = View.INVISIBLE
                true
            }
            gridPiezas.addView(img)
        }
    }

    private fun crearTableroVacio(grid: GridLayout, tipoTablero: String) {
        for (i in 0 until 24) {
            val hueco = ImageView(this)
            hueco.setBackgroundColor(Color.LTGRAY)
            val params = GridLayout.LayoutParams()
            params.width = 130
            params.height = 90
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

                            piezasAcertadas++

                            // 2. Mostrar el botón SOLO cuando se completan todas las piezas
                            if (piezasAcertadas == TOTAL_PIEZAS) {
                                findViewById<Button>(R.id.btnJarraitu).visibility = View.VISIBLE
                                mostrarSeccionFinal()
                            }
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

    private fun mostrarSeccionFinal() {
        // 3. Asegurarnos de que el layout final y el botón sean visibles
        val layoutFinal = findViewById<LinearLayout>(R.id.layoutFinal)
        layoutFinal.visibility = View.VISIBLE
        findViewById<Button>(R.id.btnJarraitu).visibility = View.VISIBLE

        val scrollView = findViewById<ScrollView>(R.id.scrollViewMain)
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }

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