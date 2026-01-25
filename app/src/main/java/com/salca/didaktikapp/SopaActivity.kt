package com.salca.didaktikapp

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.AnimationDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SopaActivity : AppCompatActivity() {

    private lateinit var mainScrollView: ScrollView
    private lateinit var contenedorIntro: LinearLayout
    private lateinit var sopaContainer: LinearLayout
    private lateinit var tvTextoIntroductorio: TextView
    private lateinit var btnComenzarSopa: Button
    private lateinit var btnVolverMapa: ImageButton

    // Audio
    private lateinit var btnPlayPauseIcon: ImageButton
    private lateinit var seekBarAudio: SeekBar
    private lateinit var runnable: Runnable
    private var handler = Handler(Looper.getMainLooper())

    // Juego
    private lateinit var wordSearchView: WordSearchView
    private lateinit var tvProgress: TextView
    private lateinit var btnFinish: Button

    // VISTAS PARA ANIMACIÓN
    private lateinit var contenedorListaPalabras: LinearLayout
    private lateinit var ivLeonAnimadoFinal: ImageView

    // ============================================
    // NUEVO: Variable para controlar la animación del león
    // ============================================
    private var animacionLeon: AnimationDrawable? = null

    private val wordToCheckbox = mutableMapOf<String, CheckBox>()
    private var foundWordsCount = 0
    private val totalWords = 7
    private var puntuacionActual = 0

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sopa)
        try {
            initializeViews()
            setupAudioControls()
            setupWordSearchView()
            setupFinishButton()
            setupAudio()
        } catch (e: Exception) {
            Toast.makeText(this, "Errorea: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // ============================================
        // NUEVO: Detener animación del león si está corriendo
        // ============================================
        animacionLeon?.stop()

        if (::runnable.isInitialized) handler.removeCallbacks(runnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun initializeViews() {
        mainScrollView = findViewById(R.id.mainScrollView)
        contenedorIntro = findViewById(R.id.contenedorIntro)
        sopaContainer = findViewById(R.id.sopaContainer)
        btnComenzarSopa = findViewById(R.id.btnComenzarSopa)
        btnVolverMapa = findViewById(R.id.btnVolverMapa)
        btnPlayPauseIcon = findViewById(R.id.btnPlayPauseIcon)
        seekBarAudio = findViewById(R.id.seekBarAudio)
        wordSearchView = findViewById(R.id.wordSearchView)
        tvProgress = findViewById(R.id.tvProgress)
        btnFinish = findViewById(R.id.btnFinish)

        // Inicialización de la animación
        contenedorListaPalabras = findViewById(R.id.contenedorListaPalabras)
        ivLeonAnimadoFinal = findViewById(R.id.ivLeonAnimadoFinal)

        // ============================================
        // NUEVO: Configurar la animación del león
        // ============================================
        setupLeonAnimation()

        // Checkboxes
        wordToCheckbox["SOMERA"] = findViewById(R.id.cbSomera)
        wordToCheckbox["ARTEKALE"] = findViewById(R.id.cbArtecalle)
        wordToCheckbox["TENDERIA"] = findViewById(R.id.cbTenderia)
        wordToCheckbox["BELOSTIKALE"] = findViewById(R.id.cbBelosticalle)
        wordToCheckbox["CARNICERIAVIEJA"] = findViewById(R.id.cbCarniceriaVieja)
        wordToCheckbox["BARRENKALE"] = findViewById(R.id.cbBarrencalle)
        wordToCheckbox["BARRENKALEBARRENA"] = findViewById(R.id.cbBarrenkaleBarrena)

        updateProgress()

        btnVolverMapa.setOnClickListener {
            if (isPlaying) pauseAudio()
            finish()
        }

        btnComenzarSopa.setOnClickListener {
            if (isPlaying) pauseAudio()
            contenedorIntro.visibility = View.GONE
            sopaContainer.visibility = View.VISIBLE
            mainScrollView.scrollTo(0, 0)
        }
    }

    // ============================================
    // NUEVO: Método para configurar la animación del león
    // Este método prepara la animación que se mostrará al finalizar el juego
    // ============================================
    private fun setupLeonAnimation() {
        // Establecer el drawable de animación al ImageView
        ivLeonAnimadoFinal.setBackgroundResource(R.drawable.animacion_celebracion)

        // Obtener la referencia a la animación
        animacionLeon = ivLeonAnimadoFinal.background as? AnimationDrawable
    }

    private fun setupAudio() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.jarduera_3)
            mediaPlayer?.setOnPreparedListener { mp -> seekBarAudio.max = mp.duration }
            mediaPlayer?.setOnCompletionListener {
                btnPlayPauseIcon.setImageResource(android.R.drawable.ic_media_play)
                seekBarAudio.progress = 0
                isPlaying = false
                if (::runnable.isInitialized) handler.removeCallbacks(runnable)
            }
        } catch (e: Exception) { }
    }

    private fun setupAudioControls() {
        btnPlayPauseIcon.setOnClickListener { if (isPlaying) pauseAudio() else playAudio() }
        seekBarAudio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun playAudio() {
        mediaPlayer?.start()
        isPlaying = true
        btnPlayPauseIcon.setImageResource(android.R.drawable.ic_media_pause)
        updateSeekBar()
    }

    private fun pauseAudio() {
        mediaPlayer?.pause()
        isPlaying = false
        btnPlayPauseIcon.setImageResource(android.R.drawable.ic_media_play)
        if (::runnable.isInitialized) handler.removeCallbacks(runnable)
    }

    private fun updateSeekBar() {
        runnable = Runnable {
            seekBarAudio.progress = mediaPlayer?.currentPosition ?: 0
            handler.postDelayed(runnable, 500)
        }
        handler.postDelayed(runnable, 0)
    }

    private fun setupWordSearchView() {
        wordSearchView.onWordFoundListener = { word, count ->
            foundWordsCount = count
            wordToCheckbox[word]?.isChecked = true
            puntuacionActual += 50
            updateProgress()
            guardarPuntuacionEnBD(puntuacionActual)
            SyncHelper.subirInmediatamente(this)
            if (foundWordsCount == totalWords) onGameCompleted()
        }
    }

    private fun setupFinishButton() {
        btnFinish.setOnClickListener {
            // ============================================
            // ANIMACIÓN FINAL - MODIFICADO para usar AnimationDrawable
            // ============================================

            // Ocultar la lista de palabras
            contenedorListaPalabras.visibility = View.GONE

            // Mostrar el león animado
            ivLeonAnimadoFinal.visibility = View.VISIBLE

            // Iniciar la animación del león
            animacionLeon?.start()

            // Esperar 4 segundos, luego guardar puntuación y cerrar
            Handler(Looper.getMainLooper()).postDelayed({
                // Detener la animación antes de salir
                animacionLeon?.stop()

                // Guardar puntuación en la base de datos
                guardarPuntuacionEnBD(puntuacionActual)
                SyncHelper.subirInmediatamente(this)

                // Cerrar la actividad
                finish()
            }, 4000)
        }
    }

    private fun updateProgress() {
        tvProgress.text = "$foundWordsCount/$totalWords"
        val isComplete = foundWordsCount == totalWords
        btnFinish.isEnabled = isComplete
        if (isComplete) {
            btnFinish.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.sopa))
        } else {
            btnFinish.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.boton_desactivado))
        }
    }

    private fun onGameCompleted() {
        puntuacionActual += 150
        guardarPuntuacionEnBD(puntuacionActual)
        SyncHelper.subirInmediatamente(this)
    }

    private fun guardarPuntuacionEnBD(puntos: Int) {
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombreAlumno = prefs.getString("nombre_alumno_actual", "Anonimo") ?: "Anonimo"
        val dbHelper = DatabaseHelper(this)
        dbHelper.guardarPuntuacion(nombreAlumno, "Sopa", puntos)
    }
}

// Clase WordSearchView
class WordSearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val gridRows = 17
    private val gridCols = 12
    private var cellSize = 0f
    private var offsetX = 0f
    private var offsetY = 0f

    private val grid = arrayOf(
        charArrayOf('C', 'S', 'O', 'M', 'E', 'R', 'A', 'K', 'Z', 'P', 'L', 'B'),
        charArrayOf('A', 'A', 'R', 'T', 'E', 'K', 'A', 'L', 'E', 'W', 'N', 'A'),
        charArrayOf('R', 'T', 'E', 'N', 'D', 'E', 'R', 'I', 'A', 'G', 'H', 'R'),
        charArrayOf('N', 'B', 'E', 'L', 'O', 'S', 'T', 'I', 'K', 'A', 'L', 'R'),
        charArrayOf('I', 'M', 'P', 'Q', 'W', 'X', 'Y', 'Z', 'U', 'V', 'D', 'E'),
        charArrayOf('C', 'B', 'A', 'R', 'R', 'E', 'N', 'K', 'A', 'L', 'E', 'N'),
        charArrayOf('E', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'K'),
        charArrayOf('R', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'A'),
        charArrayOf('I', 'Z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'L'),
        charArrayOf('A', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'E'),
        charArrayOf('V', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'A', 'B', 'C', 'B'),
        charArrayOf('I', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'A'),
        charArrayOf('E', 'U', 'V', 'W', 'X', 'Y', 'Z', 'A', 'B', 'C', 'D', 'R'),
        charArrayOf('J', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'R'),
        charArrayOf('A', 'B', 'E', 'L', 'O', 'S', 'T', 'I', 'K', 'A', 'L', 'E'),
        charArrayOf('S', 'Y', 'Z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'N'),
        charArrayOf('O', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'A')
    )

    private val foundWords = mutableSetOf<String>()
    private var isDragging = false
    private var startCell: Pair<Int, Int>? = null
    private var currentCell: Pair<Int, Int>? = null
    private val selectedCells = mutableListOf<Pair<Int, Int>>()

    private val wordColors = listOf(
        ContextCompat.getColor(context, R.color.sopa_verde),
        ContextCompat.getColor(context, R.color.sopa_azul),
        ContextCompat.getColor(context, R.color.sopa_naranja),
        ContextCompat.getColor(context, R.color.sopa_rosa),
        ContextCompat.getColor(context, R.color.sopa_purpura),
        ContextCompat.getColor(context, R.color.sopa_amarillo),
        ContextCompat.getColor(context, R.color.sopa_lima)
    )

    private val foundCellColors = mutableMapOf<Pair<Int, Int>, Int>()

    private val gridPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.sopa_rejilla)
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.sopa_texto_letras)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val highlightPaint = Paint().apply {
        style = Paint.Style.FILL
        alpha = 200
    }

    private val selectionPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.sopa_seleccion_dedo)
        style = Paint.Style.FILL
        alpha = 120
    }

    var onWordFoundListener: ((String, Int) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val gridWidth = w - 32f
        val gridHeight = h - 32f
        cellSize = min(gridWidth / gridCols, gridHeight / gridRows)
        val totalWidth = cellSize * gridCols
        val totalHeight = cellSize * gridRows
        offsetX = (w - totalWidth) / 2f
        offsetY = (h - totalHeight) / 2f
        textPaint.textSize = cellSize * 0.45f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for ((cell, color) in foundCellColors) {
            highlightPaint.color = color
            val (row, col) = cell
            canvas.drawRect(offsetX + col * cellSize, offsetY + row * cellSize, offsetX + (col + 1) * cellSize, offsetY + (row + 1) * cellSize, highlightPaint)
        }
        for (cell in selectedCells) {
            val (row, col) = cell
            canvas.drawRect(offsetX + col * cellSize, offsetY + row * cellSize, offsetX + (col + 1) * cellSize, offsetY + (row + 1) * cellSize, selectionPaint)
        }
        for (i in 0..gridCols) {
            val pos = offsetX + i * cellSize
            canvas.drawLine(pos, offsetY, pos, offsetY + gridRows * cellSize, gridPaint)
        }
        for (i in 0..gridRows) {
            val pos = offsetY + i * cellSize
            canvas.drawLine(offsetX, pos, offsetX + gridCols * cellSize, pos, gridPaint)
        }
        for (row in 0 until gridRows) {
            for (col in 0 until gridCols) {
                val x = offsetX + col * cellSize + cellSize / 2
                val y = offsetY + row * cellSize + cellSize / 2 - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(grid[row][col].toString(), x, y, textPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent.requestDisallowInterceptTouchEvent(true)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val cell = getCellFromTouch(event.x, event.y)
                if (cell != null) {
                    isDragging = true
                    startCell = cell
                    currentCell = cell
                    selectedCells.clear()
                    selectedCells.add(cell)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val cell = getCellFromTouch(event.x, event.y)
                    if (cell != null && cell != currentCell) {
                        currentCell = cell
                        updateSelection()
                        invalidate()
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    checkForWord()
                    isDragging = false
                    selectedCells.clear()
                    startCell = null
                    currentCell = null
                    invalidate()
                }
                parent.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getCellFromTouch(x: Float, y: Float): Pair<Int, Int>? {
        val col = ((x - offsetX) / cellSize).toInt()
        val row = ((y - offsetY) / cellSize).toInt()
        return if (row in 0 until gridRows && col in 0 until gridCols) Pair(row, col) else null
    }

    // LÓGICA DE SELECCIÓN SIN DIAGONALES
    private fun updateSelection() {
        val start = startCell ?: return
        val current = currentCell ?: return
        selectedCells.clear()

        val rowDiff = current.first - start.first
        val colDiff = current.second - start.second

        // Bloqueo de diagonales: forzamos el eje dominante
        val targetRow: Int
        val targetCol: Int

        if (abs(rowDiff) > abs(colDiff)) {
            targetRow = current.first
            targetCol = start.second
        } else {
            targetRow = start.first
            targetCol = current.second
        }

        val newRowDiff = targetRow - start.first
        val newColDiff = targetCol - start.second
        val steps = max(abs(newRowDiff), abs(newColDiff))

        if (steps == 0) {
            selectedCells.add(start)
            return
        }

        val rowStep = if (newRowDiff == 0) 0 else newRowDiff / abs(newRowDiff)
        val colStep = if (newColDiff == 0) 0 else newColDiff / abs(newColDiff)

        for (i in 0..steps) {
            selectedCells.add(Pair(start.first + i * rowStep, start.second + i * colStep))
        }
    }

    private fun checkForWord() {
        if (selectedCells.isEmpty()) return
        val selectedWord = buildString { for (cell in selectedCells) append(grid[cell.first][cell.second]) }
        val reversedWord = selectedWord.reversed()
        val targetWords = listOf("SOMERA", "ARTEKALE", "TENDERIA", "BELOSTIKALE", "CARNICERIAVIEJA", "BARRENKALE", "BARRENKALEBARRENA")

        for (word in targetWords) {
            if ((selectedWord == word || reversedWord == word) && !foundWords.contains(word)) {
                if (word == "BARRENKALE") {
                    val esLaVertical = selectedCells.all { it.second == 11 }
                    if (esLaVertical) continue
                }
                foundWords.add(word)
                val colorIndex = foundWords.size - 1
                val color = wordColors[colorIndex % wordColors.size]
                for (cell in selectedCells) foundCellColors[cell] = color
                onWordFoundListener?.invoke(word, foundWords.size)
                break
            }
        }
    }
}