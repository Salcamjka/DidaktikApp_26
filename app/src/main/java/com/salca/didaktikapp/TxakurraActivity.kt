package com.salca.didaktikapp

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Actividad de introducción para "Txakurraren Iturria" (La Fuente del Perro).
 *
 * Esta actividad presenta la historia y leyendas sobre la fuente del Casco Viejo
 * de Bilbao que tiene cabezas de león pero se llama "Fuente del Perro".
 *
 * Contenido educativo:
 * - Texto explicativo sobre la fuente y su ubicación
 * - Dos teorías sobre el origen del nombre:
 *   1. Los bilbaínos no conocían los leones y pensaron que eran perros
 *   2. Un vecino puso una escultura de león y la gente bromeaba que parecía perro
 * - Audio narrativo (jarduera_4.m4a)
 * - Mascota animada (león del Athletic en estado normal)
 *
 * Flujo:
 * 1. Se muestra el texto con las dos teorías
 * 2. Audio se puede reproducir con botón único
 * 3. Mascota aparece con animación bounce
 * 4. Usuario puede reproducir/pausar el audio con un solo botón
 * 5. Al presionar "Jarraitu", se abre TxakurraTablaActivity
 *
 * @author Salca
 * @version 3.1 - Error corregido
 * @since 2026-01-11
 */
class TxakurraActivity : AppCompatActivity() {

    // ============================================================================
    // COMPONENTES DE UI
    // ============================================================================

    /** Reproductor de audio para la narración histórica */
    private var mediaPlayer: MediaPlayer? = null

    /** Estado del reproductor de audio (reproduciendo/pausado) */
    private var isPlaying = false

    /** Botón para reproducir/pausar el audio */
    private lateinit var btnPlayAudio: Button

    /** Botón para continuar a la actividad de la tabla */
    private lateinit var btnContinuar: Button

    /** Mascota (león del Athletic en estado normal) */
    private lateinit var ivMascota: ImageView

    // ============================================================================
    // CICLO DE VIDA DE LA ACTIVIDAD
    // ============================================================================

    /**
     * Método llamado al crear la actividad.
     * Inicializa componentes, configura audio y anima la mascota.
     *
     * @param savedInstanceState Estado guardado de la actividad (si existe)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_txakurra)

        initializeViews()
        setupAudioButton()
        setupContinuarButton()
        setupAudio()
        animateMascota()
    }

    /**
     * Método llamado al destruir la actividad.
     * Libera los recursos del reproductor de audio.
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
     * Inicializa todas las vistas vinculándolas con sus IDs del XML.
     */
    private fun initializeViews() {
        btnPlayAudio = findViewById(R.id.btnPlayAudio)
        btnContinuar = findViewById(R.id.btnContinuar)
        ivMascota = findViewById(R.id.ivMascota)
    }

    // ============================================================================
    // ANIMACIONES DE LA MASCOTA
    // ============================================================================

    /**
     * Anima la mascota con efecto de entrada rebotando.
     * Se ejecuta automáticamente al crear la actividad.
     */
    private fun animateMascota() {
        val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_bounce_in)
        ivMascota.startAnimation(bounceAnim)
    }

    // ============================================================================
    // GESTIÓN DE AUDIO SIMPLIFICADA
    // ============================================================================

    /**
     * Configura el reproductor de audio (sin reproducción automática).
     *
     * El audio (jarduera_4.m4a) narra la historia de la fuente y se reproduce
     * en bucle cuando el usuario presiona el botón.
     */
    private fun setupAudio() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.jarduera_4)
            mediaPlayer?.isLooping = true
        } catch (e: Exception) {
            Toast.makeText(this, "Audio no disponible", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Configura el botón de reproducción/pausa del audio.
     * Alterna entre reproducir y pausar según el estado actual.
     */
    private fun setupAudioButton() {
        btnPlayAudio.setOnClickListener {
            if (isPlaying) {
                pauseAudio()
            } else {
                playAudio()
            }
        }
    }

    /**
     * Reproduce el audio narrativo.
     * Cambia el texto del botón a "⏸️ Pausatu".
     */
    private fun playAudio() {
        try {
            mediaPlayer?.start()
            isPlaying = true
            updateAudioButton()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al reproducir audio", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Pausa el audio narrativo.
     * Cambia el texto del botón a "🔊 Entzun audioa".
     */
    private fun pauseAudio() {
        mediaPlayer?.pause()
        isPlaying = false
        updateAudioButton()
    }

    /**
     * Actualiza el texto del botón según el estado del audio.
     */
    private fun updateAudioButton() {
        if (isPlaying) {
            btnPlayAudio.text = "⏸️ Pausatu"
        } else {
            btnPlayAudio.text = "🔊 Entzun audioa"
        }

        // Mantener color negro siempre usando ColorStateList directamente
        btnPlayAudio.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.BLACK
        )
    }

    // ============================================================================
    // NAVEGACIÓN
    // ============================================================================

    /**
     * Configura el botón "Jarraitu" para navegar a la actividad de la tabla.
     *
     * Al presionarlo:
     * 1. Detiene y libera el reproductor de audio
     * 2. Abre TxakurraTablaActivity (actividad de la tabla comparativa)
     */
    private fun setupContinuarButton() {
        btnContinuar.setOnClickListener {
            // Detener audio
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false

            // Abrir actividad de la tabla
            val intent = Intent(this, TxakurraTablaActivity::class.java)
            startActivity(intent)
        }
    }
}