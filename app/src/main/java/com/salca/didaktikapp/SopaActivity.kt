package com.salca.didaktikapp

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity

/**
 * Actividad de sopa de letras para la ubicación "Zazpi Kaleak" (Las Siete Calles).
 *
 * Esta actividad presenta un juego educativo donde los estudiantes deben encontrar
 * los nombres de las siete calles históricas del Casco Viejo de Bilbao en una
 * sopa de letras interactiva.
 *
 * La mascota (león del Athletic) acompaña al estudiante durante toda la actividad:
 * - Pantalla 1: Mascota normal
 * - Pantalla 2: Mascota saludando mientras juega
 * - Cuando encuentra una palabra: Mascota celebra brevemente
 * - Cuando completa todo: Mascota celebra con animación completa
 *
 * Flujo de la actividad:
 * 1. Pantalla inicial con texto histórico y audio explicativo
 * 2. Al presionar "Hasi Sopa de Letras", se muestra la sopa de letras
 * 3. Los estudiantes buscan 7 palabras: Somera, Artekale, Tendería, Belostikale,
 *    Carniceria Vieja, Barrenkale, Barrenkale Barrena
 * 4. Al encontrar todas las palabras, se habilita el botón "Amaitu"
 *
 * @author Salca
 * @version 2.0
 * @since 2026-01-07
 */
class SopaActivity : AppCompatActivity() {

    // ============ COMPONENTES DE UI - PANTALLA 1 (TEXTO) ============

    /** Contenedor principal de la pantalla de texto introductorio */
    private lateinit var scrollTextContainer: ScrollView

    /** TextView que muestra el texto histórico sobre las Zazpi Kaleak */
    private lateinit var tvTextoIntroductorio: TextView

    /** Botón para comenzar la sopa de letras */
    private lateinit var btnComenzarSopa: Button

    /** Mascota en la pantalla de texto (león normal) */
    private lateinit var ivMascotaPantalla1: ImageView

    // ============ COMPONENTES DE UI - PANTALLA 2 (SOPA) ============

    /** Vista personalizada que contiene la cuadrícula de la sopa de letras */
    private lateinit var wordSearchView: WordSearchView

    /** TextView que muestra el progreso (X/7 palabras encontradas) */
    private lateinit var tvProgress: TextView

    /** Botón para finalizar la actividad (solo habilitado al completar) */
    private lateinit var btnFinish: Button

    /** Contenedor principal de la pantalla de sopa de letras */
    private lateinit var sopaContainer: ScrollView

    /** Mascota en la pantalla de juego (cambia según el estado) */
    private lateinit var ivMascotaPantalla2: ImageView

    // ============ CHECKBOXES PARA PALABRAS ENCONTRADAS ============

    /** CheckBox para la palabra "Barrenkale" */
    private lateinit var cbBarrencalle: CheckBox

    /** CheckBox para la palabra "Belostikale" */
    private lateinit var cbBelosticalle: CheckBox

    /** CheckBox para la palabra "Carnicería Vieja" */
    private lateinit var cbCarniceriaVieja: CheckBox

    /** CheckBox para la palabra "Somera" */
    private lateinit var cbSomera: CheckBox

    /** CheckBox para la palabra "Artekale" */
    private lateinit var cbArtecalle: CheckBox

    /** CheckBox para la palabra "Tendería" */
    private lateinit var cbTenderia: CheckBox

    /** CheckBox para la palabra "Barrenkale Barrena" */
    private lateinit var cbBarrenkaleBarrena: CheckBox

    // ============ ESTADO DEL JUEGO ============

    /**
     * Mapa que relaciona cada palabra (en mayúsculas) con su CheckBox correspondiente
     * Permite marcar automáticamente el checkbox cuando se encuentra una palabra
     */
    private val wordToCheckbox = mutableMapOf<String, CheckBox>()

    /** Contador de palabras encontradas por el jugador */
    private var foundWordsCount = 0

    /** Número total de palabras a encontrar en la sopa de letras */
    private val totalWords = 7

    // ============ REPRODUCTOR DE AUDIO ============

    /** Reproductor de audio para el texto introductorio (jarduera_3.m4a) */
    private var mediaPlayer: MediaPlayer? = null

    // ============================================================================
    // CICLO DE VIDA DE LA ACTIVIDAD
    // ============================================================================

    /**
     * Método llamado al crear la actividad.
     * Inicializa todos los componentes, configura el audio y muestra la pantalla inicial.
     *
     * @param savedInstanceState Estado guardado de la actividad (si existe)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sopa)

        initializeViews()
        setupAudio()
        setupWordSearchView()
        setupFinishButton()

        mostrarPantallaTexto()
        animateMascotaInicial()
    }

    /**
     * Método llamado al destruir la actividad.
     * Libera los recursos del reproductor de audio para evitar memory leaks.
     */
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // ============================================================================
    // INICIALIZACIÓN DE COMPONENTES
    // ============================================================================

    /**
     * Inicializa todas las vistas y componentes de la interfaz.
     *
     * Pasos:
     * 1. Vincula las vistas XML con las variables Kotlin mediante findViewById
     * 2. Crea el mapa palabra-checkbox para automatizar las marcas
     * 3. Actualiza el progreso inicial (0/7)
     * 4. Configura el listener del botón para iniciar la sopa
     */
    private fun initializeViews() {
        // Pantalla 1: Texto introductorio
        scrollTextContainer = findViewById(R.id.scrollTextContainer)
        tvTextoIntroductorio = findViewById(R.id.tvTextoIntroductorio)
        btnComenzarSopa = findViewById(R.id.btnComenzarSopa)
        ivMascotaPantalla1 = findViewById(R.id.ivMascotaPantalla1)

        // Pantalla 2: Sopa de letras
        sopaContainer = findViewById(R.id.sopaContainer)
        wordSearchView = findViewById(R.id.wordSearchView)
        tvProgress = findViewById(R.id.tvProgress)
        btnFinish = findViewById(R.id.btnFinish)
        ivMascotaPantalla2 = findViewById(R.id.ivMascotaPantalla2)

        // Checkboxes de palabras
        cbBarrencalle = findViewById(R.id.cbBarrencalle)
        cbBelosticalle = findViewById(R.id.cbBelosticalle)
        cbCarniceriaVieja = findViewById(R.id.cbCarniceriaVieja)
        cbSomera = findViewById(R.id.cbSomera)
        cbArtecalle = findViewById(R.id.cbArtecalle)
        cbTenderia = findViewById(R.id.cbTenderia)
        cbBarrenkaleBarrena = findViewById(R.id.cbBarrenkaleBarrena)

        // Mapeo palabra -> checkbox
        wordToCheckbox["SOMERA"] = cbSomera
        wordToCheckbox["ARTEKALE"] = cbArtecalle
        wordToCheckbox["TENDERIA"] = cbTenderia
        wordToCheckbox["BELOSTIKALE"] = cbBelosticalle
        wordToCheckbox["CARNICERIAVIEJA"] = cbCarniceriaVieja
        wordToCheckbox["BARRENKALE"] = cbBarrencalle
        wordToCheckbox["BARRENKALEBARRENA"] = cbBarrenkaleBarrena

        updateProgress()

        // Listener del botón para comenzar la sopa
        btnComenzarSopa.setOnClickListener {
            mostrarSopaDeLetras()
        }
    }

    // ============================================================================
    // ANIMACIONES DE LA MASCOTA
    // ============================================================================

    /**
     * Anima la mascota inicial (pantalla de texto) con efecto de entrada rebotando.
     * Se ejecuta automáticamente al crear la actividad.
     */
    private fun animateMascotaInicial() {
        val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_bounce_in)
        ivMascotaPantalla1.startAnimation(bounceAnim)
    }

    /**
     * Anima la mascota de la pantalla de juego con efecto de saludo.
     * Se ejecuta cuando el usuario entra a la sopa de letras.
     */
    private fun animateMascotaSaludando() {
        val waveAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_wave)
        ivMascotaPantalla2.startAnimation(waveAnim)
    }

    /**
     * Anima brevemente la mascota cuando el usuario encuentra una palabra.
     * Usa una animación de celebración corta para no interrumpir el juego.
     */
    private fun animateMascotaPalabraEncontrada() {
        val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_bounce_in)
        ivMascotaPantalla2.startAnimation(bounceAnim)
    }

    /**
     * Cambia la mascota a estado de celebración y ejecuta animación completa.
     * Se ejecuta cuando el usuario completa todas las palabras.
     */
    private fun animateMascotaCelebracion() {
        // Cambiar imagen a mascota celebrando
        ivMascotaPantalla2.setImageResource(R.drawable.mascota_celebrando)

        // Animación de celebración completa
        val celebrateAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_celebrate)
        ivMascotaPantalla2.startAnimation(celebrateAnim)
    }

    // ============================================================================
    // CONTROL DE PANTALLAS
    // ============================================================================

    /**
     * Muestra la pantalla de texto introductorio y oculta la sopa de letras.
     * Esta es la pantalla inicial de la actividad.
     */
    private fun mostrarPantallaTexto() {
        scrollTextContainer.visibility = View.VISIBLE
        sopaContainer.visibility = View.GONE
    }

    /**
     * Muestra la sopa de letras y oculta la pantalla de texto.
     * También detiene y libera el reproductor de audio.
     * Anima la mascota saludando al entrar.
     */
    private fun mostrarSopaDeLetras() {
        scrollTextContainer.visibility = View.GONE
        sopaContainer.visibility = View.VISIBLE

        // Detener el audio al pasar a la sopa
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        // Animar mascota saludando
        animateMascotaSaludando()
    }

    // ============================================================================
    // GESTIÓN DE AUDIO
    // ============================================================================

    /**
     * Configura y reproduce el audio introductorio en loop.
     *
     * El audio explica la historia de las Zazpi Kaleak y se reproduce
     * automáticamente en bucle hasta que el usuario presiona "Hasi Sopa de Letras".
     *
     * Si el archivo de audio no está disponible, muestra un Toast informativo.
     */
    private fun setupAudio() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.jarduera_3)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        } catch (e: Exception) {
            Toast.makeText(this, "Audio no disponible", Toast.LENGTH_SHORT).show()
        }
    }

    // ============================================================================
    // LÓGICA DE LA SOPA DE LETRAS
    // ============================================================================

    /**
     * Configura el listener de la vista de sopa de letras.
     *
     * Este listener se ejecuta cada vez que el usuario encuentra una palabra válida:
     * 1. Actualiza el contador de palabras encontradas
     * 2. Marca el checkbox correspondiente
     * 3. Actualiza el indicador de progreso
     * 4. Muestra un mensaje de confirmación
     * 5. Anima la mascota brevemente
     * 6. Si todas las palabras están encontradas, ejecuta onGameCompleted()
     *
     * @see WordSearchView.onWordFoundListener
     */
    private fun setupWordSearchView() {
        wordSearchView.onWordFoundListener = { word, count ->
            foundWordsCount = count

            // Marcar el checkbox de la palabra encontrada
            wordToCheckbox[word]?.isChecked = true

            updateProgress()
            showWordFoundMessage(word)

            // Animar mascota al encontrar palabra
            animateMascotaPalabraEncontrada()

            // Verificar si se completó el juego
            if (foundWordsCount == totalWords) {
                onGameCompleted()
            }
        }
    }

    /**
     * Configura el botón de finalización.
     * Al presionarlo, cierra la actividad y regresa al mapa.
     */
    private fun setupFinishButton() {
        btnFinish.setOnClickListener {
            finish()
        }
    }

    /**
     * Actualiza el indicador de progreso (X/7) y el estado del botón "Amaitu".
     *
     * El botón "Amaitu" solo se habilita cuando se encuentran las 7 palabras,
     * y cambia su color a verde para indicar que el juego está completado.
     */
    private fun updateProgress() {
        tvProgress.text = "$foundWordsCount/$totalWords"

        // Habilitar botón solo si se completó
        btnFinish.isEnabled = foundWordsCount == totalWords

        // Cambiar color a verde si está completo
        if (foundWordsCount == totalWords) {
            btnFinish.backgroundTintList = getColorStateList(android.R.color.holo_green_light)
        }
    }

    /**
     * Muestra un mensaje Toast cuando se encuentra una palabra.
     *
     * Convierte el nombre de la palabra desde el formato interno (MAYÚSCULAS, SIN ESPACIOS)
     * al formato de visualización (capitalizado, con espacios).
     *
     * @param word Palabra encontrada en formato interno (ej: "CARNICERIAVIEJA")
     */
    private fun showWordFoundMessage(word: String) {
        val displayName = when (word) {
            "SOMERA" -> "Somera"
            "ARTEKALE" -> "Artekale"
            "TENDERIA" -> "Tendería"
            "BELOSTIKALE" -> "Belostikale"
            "CARNICERIAVIEJA" -> "Carnicería Vieja"
            "BARRENKALE" -> "Barrenkale"
            "BARRENKALEBARRENA" -> "Barrenkale Barrena"
            else -> word
        }

        Toast.makeText(
            this,
            "✓ $displayName aurkituta!",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Método llamado cuando el jugador encuentra todas las palabras.
     * Muestra un mensaje de felicitación en euskera y anima la mascota celebrando.
     */
    private fun onGameCompleted() {
        // Animar mascota celebrando
        animateMascotaCelebracion()

        Toast.makeText(
            this,
            "🎉 Zorionak! Hitz guztiak aurkitu dituzu!",
            Toast.LENGTH_LONG
        ).show()
    }
}