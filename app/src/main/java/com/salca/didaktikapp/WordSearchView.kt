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
 * Vista personalizada para sopa de letras - VERSIÓN LIMPIA Y MEJORADA
 *
 * Cuadrícula 17x12 con TODAS las 7 palabras completas
 * Diseño limpio con colores sutiles y profesionales
 *
 * @author Salca - TXO Team
 * @version 4.0 - DISEÑO LIMPIO
 * @since 2026-01-11
 */
class WordSearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ============================================================================
    // CONFIGURACIÓN DE LA CUADRÍCULA
    // ============================================================================

    private val gridRows = 17  // 17 filas para palabras largas
    private val gridCols = 12  // 12 columnas

    private var cellSize = 0f
    private var offsetX = 0f
    private var offsetY = 0f

    /**
     * Cuadrícula 17x12 con LAS 7 PALABRAS COMPLETAS
     *
     * TODAS LAS PALABRAS:
     * - Fila 0:  SOMERA (horizontal, columnas 1-6)
     * - Fila 1:  ARTEKALE (horizontal, columnas 0-7)
     * - Fila 2:  TENDERIA (horizontal, columnas 1-8)
     * - Fila 3:  BELOSTIKALE (horizontal, columnas 1-11)
     * - Fila 5:  BARRENKALE (horizontal, columnas 1-10)
     * - Col 0:   CARNICERIAVIEJA (vertical, 16 letras, filas 0-15)
     * - Col 11:  BARRENKALEBARRENA (vertical, 17 letras, filas 0-16)
     */
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
        charArrayOf('A', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'E'),
        charArrayOf('S', 'Y', 'Z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'N'),
        charArrayOf('O', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'A')
    )

    // ============================================================================
    // ESTADO DEL JUEGO
    // ============================================================================

    private val foundWords = mutableSetOf<String>()
    private var isDragging = false
    private var startCell: Pair<Int, Int>? = null
    private var currentCell: Pair<Int, Int>? = null
    private val selectedCells = mutableListOf<Pair<Int, Int>>()

    // ============================================================================
    // COLORES Y ESTILOS - DISEÑO LIMPIO
    // ============================================================================

    // Colores sutiles y profesionales
    private val wordColors = listOf(
        Color.parseColor("#C8E6C9"),  // Verde suave
        Color.parseColor("#B3E5FC"),  // Azul claro
        Color.parseColor("#FFE0B2"),  // Naranja claro
        Color.parseColor("#F8BBD0"),  // Rosa suave
        Color.parseColor("#D1C4E9"),  // Púrpura claro
        Color.parseColor("#FFF9C4"),  // Amarillo suave
        Color.parseColor("#DCEDC8")   // Lima claro
    )

    private val foundCellColors = mutableMapOf<Pair<Int, Int>, Int>()

    private val gridPaint = Paint().apply {
        color = Color.parseColor("#E0E0E0")
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint().apply {
        color = Color.parseColor("#424242")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val highlightPaint = Paint().apply {
        style = Paint.Style.FILL
        alpha = 200
    }

    private val selectionPaint = Paint().apply {
        color = Color.parseColor("#FFEB3B")
        style = Paint.Style.FILL
        alpha = 120
    }

    var onWordFoundListener: ((String, Int) -> Unit)? = null

    // ============================================================================
    // MÉTODOS DEL CICLO DE VIDA
    // ============================================================================

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

        // 1. Palabras encontradas
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

        // 2. Selección actual
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

        // 3. Líneas de la cuadrícula
        for (i in 0..gridCols) {
            val pos = offsetX + i * cellSize
            canvas.drawLine(pos, offsetY, pos, offsetY + gridRows * cellSize, gridPaint)
        }
        for (i in 0..gridRows) {
            val pos = offsetY + i * cellSize
            canvas.drawLine(offsetX, pos, offsetX + gridCols * cellSize, pos, gridPaint)
        }

        // 4. Letras
        for (row in 0 until gridRows) {
            for (col in 0 until gridCols) {
                val x = offsetX + col * cellSize + cellSize / 2
                val y = offsetY + row * cellSize + cellSize / 2 - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(grid[row][col].toString(), x, y, textPaint)
            }
        }
    }

    // ============================================================================
    // GESTIÓN DE EVENTOS TÁCTILES
    // ============================================================================

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
        return if (row in 0 until gridRows && col in 0 until gridCols) {
            Pair(row, col)
        } else null
    }

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

        for (i in 0..steps) {
            selectedCells.add(Pair(start.first + i * rowStep, start.second + i * colStep))
        }
    }

    private fun checkForWord() {
        if (selectedCells.isEmpty()) return

        val selectedWord = buildString {
            for (cell in selectedCells) {
                append(grid[cell.first][cell.second])
            }
        }

        val reversedWord = selectedWord.reversed()

        val targetWords = listOf(
            "SOMERA",
            "ARTEKALE",
            "TENDERIA",
            "BELOSTIKALE",
            "CARNICERIAVIEJA",
            "BARRENKALE",
            "BARRENKALEBARRENA"
        )

        for (word in targetWords) {
            if ((selectedWord == word || reversedWord == word) && !foundWords.contains(word)) {
                foundWords.add(word)
                val colorIndex = foundWords.size - 1
                val color = wordColors[colorIndex % wordColors.size]
                for (cell in selectedCells) {
                    foundCellColors[cell] = color
                }
                onWordFoundListener?.invoke(word, foundWords.size)
                break
            }
        }
    }
}