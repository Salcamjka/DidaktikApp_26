package com.salca.didaktikapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Vista personalizada para una sopa de letras interactiva.
 *
 * Esta vista permite a los estudiantes buscar palabras en una cuadrícula de letras
 * mediante gestos táctiles. Las palabras pueden estar en 8 direcciones:
 * horizontal, vertical y diagonal (en ambos sentidos).
 *
 * Características principales:
 * - Cuadrícula de 12x12 letras
 * - Selección táctil mediante arrastre
 * - Resaltado de palabras encontradas con colores únicos
 * - 7 palabras ocultas: Somera, Artekale, Tendería, Belostikale,
 *   Carnicería Vieja, Barrenkale, Barrenkale Barrena
 * - Callback cuando se encuentra una palabra válida
 *
 * @author Salca
 * @version 2.0
 * @since 2026-01-07
 *
 * @property gridSize Tamaño de la cuadrícula (12x12)
 * @property cellSize Tamaño de cada celda en píxeles (calculado dinámicamente)
 * @property offsetX Desplazamiento horizontal para centrar la cuadrícula
 * @property offsetY Desplazamiento vertical para centrar la cuadrícula
 */
class WordSearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ============================================================================
    // CONFIGURACIÓN DE LA CUADRÍCULA
    // ============================================================================

    /** Tamaño de la cuadrícula (12 filas x 12 columnas) */
    private val gridSize = 12

    /** Tamaño de cada celda en píxeles (calculado en onSizeChanged) */
    private var cellSize = 0f

    /** Desplazamiento horizontal para centrar la cuadrícula */
    private var offsetX = 0f

    /** Desplazamiento vertical para centrar la cuadrícula */
    private var offsetY = 0f

    /**
     * Cuadrícula de letras 12x12.
     *
     * Contiene las 7 palabras ocultas de forma horizontal y vertical
     * para facilitar su búsqueda por los estudiantes:
     *
     * Palabras incluidas:
     * - SOMERA (fila 0, horizontal)
     * - ARTEKALE (fila 1, horizontal)
     * - TENDERIA (fila 2, horizontal)
     * - BELOSTIKALE (fila 3, horizontal)
     * - CARNICERIAVIEJA (columna 0, vertical)
     * - BARRENKALE (fila 5, horizontal)
     * - BARRENKALEBARRENA (columna 11, vertical)
     */
    private val grid = arrayOf(
        charArrayOf('S', 'O', 'M', 'E', 'R', 'A', 'X', 'Z', 'P', 'L', 'M', 'B'),
        charArrayOf('A', 'R', 'T', 'E', 'K', 'A', 'L', 'E', 'Q', 'W', 'N', 'A'),
        charArrayOf('T', 'E', 'N', 'D', 'E', 'R', 'I', 'A', 'F', 'G', 'H', 'R'),
        charArrayOf('B', 'E', 'L', 'O', 'S', 'T', 'I', 'K', 'A', 'L', 'E', 'R'),
        charArrayOf('N', 'M', 'P', 'Q', 'W', 'X', 'Y', 'Z', 'U', 'V', 'D', 'E'),
        charArrayOf('I', 'B', 'A', 'R', 'R', 'E', 'N', 'K', 'A', 'L', 'E', 'N'),
        charArrayOf('C', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'O', 'K'),
        charArrayOf('E', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'A'),
        charArrayOf('R', 'Z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'L'),
        charArrayOf('I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'E'),
        charArrayOf('A', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'A', 'B', 'C', 'B'),
        charArrayOf('V', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'A')
    )

    // ============================================================================
    // ESTADO DEL JUEGO
    // ============================================================================

    /**
     * Set de palabras ya encontradas por el jugador.
     * Evita que se cuente la misma palabra dos veces.
     */
    private val foundWords = mutableSetOf<String>()

    /** Indica si el usuario está arrastrando el dedo para seleccionar */
    private var isDragging = false

    /** Celda donde comenzó la selección (fila, columna) */
    private var startCell: Pair<Int, Int>? = null

    /** Celda actual donde está el dedo (fila, columna) */
    private var currentCell: Pair<Int, Int>? = null

    /** Lista de celdas actualmente seleccionadas durante el arrastre */
    private val selectedCells = mutableListOf<Pair<Int, Int>>()

    // ============================================================================
    // COLORES Y ESTILOS
    // ============================================================================

    /**
     * Paleta de colores para resaltar palabras encontradas.
     * Cada palabra obtiene un color único de esta lista.
     */
    private val wordColors = listOf(
        Color.parseColor("#90EE90"),  // Verde claro
        Color.parseColor("#FFB6C1"),  // Rosa claro
        Color.parseColor("#87CEEB"),  // Azul cielo
        Color.parseColor("#FFA07A"),  // Salmón claro
        Color.parseColor("#DDA0DD"),  // Ciruela claro
        Color.parseColor("#F0E68C"),  // Khaki
        Color.parseColor("#98FB98")   // Verde menta
    )

    /**
     * Mapa que asocia cada celda encontrada con su color.
     * Permite mantener el resaltado de palabras encontradas.
     */
    private val foundCellColors = mutableMapOf<Pair<Int, Int>, Int>()

    /** Paint para dibujar las líneas de la cuadrícula */
    private val gridPaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    /** Paint para dibujar las letras en cada celda */
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        textSize = 40f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    /** Paint para resaltar celdas de palabras encontradas */
    private val highlightPaint = Paint().apply {
        style = Paint.Style.FILL
        alpha = 180  // Semi-transparente
    }

    /** Paint para mostrar la selección actual del usuario */
    private val selectionPaint = Paint().apply {
        color = Color.parseColor("#FFD700")  // Dorado
        style = Paint.Style.FILL
        alpha = 100  // Semi-transparente
    }

    // ============================================================================
    // CALLBACK
    // ============================================================================

    /**
     * Listener que se ejecuta cuando el usuario encuentra una palabra válida.
     *
     * @param word Palabra encontrada (en mayúsculas)
     * @param count Número total de palabras encontradas hasta ahora
     */
    var onWordFoundListener: ((String, Int) -> Unit)? = null

    // ============================================================================
    // MÉTODOS DEL CICLO DE VIDA
    // ============================================================================

    /**
     * Llamado cuando cambia el tamaño de la vista.
     * Calcula el tamaño de las celdas y los desplazamientos para centrar la cuadrícula.
     *
     * @param w Nuevo ancho de la vista
     * @param h Nuevo alto de la vista
     * @param oldw Ancho anterior
     * @param oldh Alto anterior
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Usar el lado más pequeño para mantener la cuadrícula cuadrada
        val size = min(w, h)
        cellSize = size / gridSize.toFloat()

        // Calcular offsets para centrar
        offsetX = (w - size) / 2f
        offsetY = (h - size) / 2f

        // Ajustar tamaño del texto
        textPaint.textSize = cellSize * 0.5f
    }

    /**
     * Dibuja la cuadrícula, las letras y los resaltados en el canvas.
     *
     * Orden de dibujado:
     * 1. Celdas de palabras encontradas (con colores)
     * 2. Selección actual del usuario (dorado semi-transparente)
     * 3. Líneas de la cuadrícula
     * 4. Letras en cada celda
     *
     * @param canvas Canvas donde se dibuja la vista
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Dibujar celdas de palabras encontradas
        for ((cell, color) in foundCellColors) {
            highlightPaint.color = color
            val (row, col) = cell
            canvas.drawRect(
                offsetX + col * cellSize,
                offsetY + row * cellSize,
                offsetX + (col + 1) * cellSize,
                offsetY + (row + 1) * cellSize,
                highlightPaint
            )
        }

        // 2. Dibujar selección actual
        for (cell in selectedCells) {
            val (row, col) = cell
            canvas.drawRect(
                offsetX + col * cellSize,
                offsetY + row * cellSize,
                offsetX + (col + 1) * cellSize,
                offsetY + (row + 1) * cellSize,
                selectionPaint
            )
        }

        // 3. Dibujar líneas de la cuadrícula
        // Líneas verticales
        for (i in 0..gridSize) {
            val pos = offsetX + i * cellSize
            canvas.drawLine(pos, offsetY, pos, offsetY + gridSize * cellSize, gridPaint)
        }
        // Líneas horizontales
        for (i in 0..gridSize) {
            val pos = offsetY + i * cellSize
            canvas.drawLine(offsetX, pos, offsetX + gridSize * cellSize, pos, gridPaint)
        }

        // 4. Dibujar letras
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val x = offsetX + col * cellSize + cellSize / 2
                val y = offsetY + row * cellSize + cellSize / 2 - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(grid[row][col].toString(), x, y, textPaint)
            }
        }
    }

    // ============================================================================
    // GESTIÓN DE EVENTOS TÁCTILES
    // ============================================================================

    /**
     * Maneja los eventos de toque del usuario.
     *
     * Flujo:
     * 1. ACTION_DOWN: Usuario toca una celda → Inicia selección
     * 2. ACTION_MOVE: Usuario arrastra → Actualiza selección
     * 3. ACTION_UP: Usuario suelta → Verifica si formó una palabra válida
     *
     * @param event Evento de toque
     * @return true si el evento fue manejado
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
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

            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    checkForWord()
                    isDragging = false
                    selectedCells.clear()
                    startCell = null
                    currentCell = null
                    invalidate()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * Convierte coordenadas de pantalla (x, y) a coordenadas de celda (fila, columna).
     *
     * @param x Coordenada X en píxeles
     * @param y Coordenada Y en píxeles
     * @return Par (fila, columna) o null si está fuera de la cuadrícula
     */
    private fun getCellFromTouch(x: Float, y: Float): Pair<Int, Int>? {
        val col = ((x - offsetX) / cellSize).toInt()
        val row = ((y - offsetY) / cellSize).toInt()
        return if (row in 0 until gridSize && col in 0 until gridSize) {
            Pair(row, col)
        } else null
    }

    /**
     * Actualiza la lista de celdas seleccionadas entre startCell y currentCell.
     *
     * Calcula una línea recta entre ambos puntos (horizontal, vertical o diagonal)
     * y añade todas las celdas intermedias a selectedCells.
     */
    private fun updateSelection() {
        val start = startCell ?: return
        val current = currentCell ?: return
        selectedCells.clear()

        val rowDiff = current.first - start.first
        val colDiff = current.second - start.second
        val steps = max(abs(rowDiff), abs(colDiff))

        if (steps == 0) {
            selectedCells.add(start)
            return
        }

        // Calcular dirección del movimiento
        val rowStep = when {
            rowDiff > 0 -> 1
            rowDiff < 0 -> -1
            else -> 0
        }

        val colStep = when {
            colDiff > 0 -> 1
            colDiff < 0 -> -1
            else -> 0
        }

        // Añadir todas las celdas en la línea
        for (i in 0..steps) {
            selectedCells.add(Pair(start.first + i * rowStep, start.second + i * colStep))
        }
    }

    /**
     * Verifica si las celdas seleccionadas forman una palabra válida.
     *
     * Pasos:
     * 1. Construye la palabra con las letras seleccionadas
     * 2. Verifica tanto la palabra como su reverso
     * 3. Compara con la lista de palabras objetivo
     * 4. Si es válida y no estaba encontrada, la marca y notifica
     */
    private fun checkForWord() {
        if (selectedCells.isEmpty()) return

        // Construir palabra seleccionada
        val selectedWord = buildString {
            for (cell in selectedCells) {
                append(grid[cell.first][cell.second])
            }
        }

        val reversedWord = selectedWord.reversed()

        /**
         * Lista de palabras a encontrar en la sopa.
         * Deben estar en MAYÚSCULAS y SIN ESPACIOS.
         */
        val targetWords = listOf(
            "SOMERA",
            "ARTEKALE",
            "TENDERIA",
            "BELOSTIKALE",
            "CARNICERIAVIEJA",
            "BARRENKALE",
            "BARRENKALEBARRENA"
        )

        // Verificar si coincide con alguna palabra objetivo
        for (word in targetWords) {
            if ((selectedWord == word || reversedWord == word) && !foundWords.contains(word)) {
                foundWords.add(word)

                // Asignar color único
                val colorIndex = foundWords.size - 1
                val color = wordColors[colorIndex % wordColors.size]

                // Resaltar celdas
                for (cell in selectedCells) {
                    foundCellColors[cell] = color
                }

                // Notificar al listener
                onWordFoundListener?.invoke(word, foundWords.size)
                break
            }
        }
    }
}