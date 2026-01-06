package com.salca.didaktikapp

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Actividad de tabla comparativa para "Txakurraren Iturria".
 *
 * Esta actividad presenta un ejercicio interactivo donde los estudiantes
 * deben comparar las características de un león y un perro en una tabla.
 *
 * Objetivo pedagógico:
 * Ayudar a los estudiantes a entender por qué los bilbaínos podían confundir
 * un león con un perro, identificando similitudes y diferencias entre ambos animales.
 *
 * Componentes visuales:
 * - Mascota animada (león del Athletic) que reacciona a las acciones
 * - Imágenes de referencia (león y perro)
 * - Tabla de 5 filas x 2 columnas para escribir características
 * - Sistema de validación que verifica que todos los campos estén completos
 *
 * Estados de la mascota:
 * - Inicio: mascota_saludando (animación wave)
 * - Tabla completa: mascota_celebrando (animación celebrate)
 * - Tabla incompleta: mascota_triste (animación shake)
 *
 * @author Salca
 * @version 2.0
 * @since 2026-01-07
 */
class TxakurraTablaActivity : AppCompatActivity() {

    // ============================================================================
    // COMPONENTES DE UI
    // ============================================================================

    /** Botón para finalizar la actividad (valida que la tabla esté completa) */
    private lateinit var btnFinish: Button

    /** Mascota que reacciona según el estado del ejercicio */
    private lateinit var ivMascota: ImageView

    /** Imagen de referencia del perro */
    private lateinit var ivPerro: ImageView

    /** Imagen de referencia del león */
    private lateinit var ivLeon: ImageView

    // ============ CAMPOS DE TEXTO - COLUMNA LEHOIA (LEÓN) ============

    /** Campo 1 de características del león */
    private lateinit var etLehoia1: EditText

    /** Campo 2 de características del león */
    private lateinit var etLehoia2: EditText

    /** Campo 3 de características del león */
    private lateinit var etLehoia3: EditText

    /** Campo 4 de características del león */
    private lateinit var etLehoia4: EditText

    /** Campo 5 de características del león */
    private lateinit var etLehoia5: EditText

    // ============ CAMPOS DE TEXTO - COLUMNA TXAKURRA (PERRO) ============

    /** Campo 1 de características del perro */
    private lateinit var etTxakurra1: EditText

    /** Campo 2 de características del perro */
    private lateinit var etTxakurra2: EditText

    /** Campo 3 de características del perro */
    private lateinit var etTxakurra3: EditText

    /** Campo 4 de características del perro */
    private lateinit var etTxakurra4: EditText

    /** Campo 5 de características del perro */
    private lateinit var etTxakurra5: EditText

    // ============================================================================
    // CICLO DE VIDA DE LA ACTIVIDAD
    // ============================================================================

    /**
     * Método llamado al crear la actividad.
     * Inicializa componentes, configura validación y anima elementos visuales.
     *
     * @param savedInstanceState Estado guardado de la actividad (si existe)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_txakurra_tabla)

        initializeViews()
        setupFinishButton()
        animateInitialElements()
    }

    // ============================================================================
    // INICIALIZACIÓN DE COMPONENTES
    // ============================================================================

    /**
     * Inicializa todas las vistas vinculándolas con sus IDs del XML.
     *
     * Componentes inicializados:
     * - Botón de finalización
     * - Mascota y imágenes de referencia
     * - 10 campos EditText (5 para león, 5 para perro)
     */
    private fun initializeViews() {
        btnFinish = findViewById(R.id.btnFinish)

        ivMascota = findViewById(R.id.ivMascota)
        ivPerro = findViewById(R.id.ivPerro)
        ivLeon = findViewById(R.id.ivLeon)

        // Campos del león
        etLehoia1 = findViewById(R.id.etLehoia1)
        etLehoia2 = findViewById(R.id.etLehoia2)
        etLehoia3 = findViewById(R.id.etLehoia3)
        etLehoia4 = findViewById(R.id.etLehoia4)
        etLehoia5 = findViewById(R.id.etLehoia5)

        // Campos del perro
        etTxakurra1 = findViewById(R.id.etTxakurra1)
        etTxakurra2 = findViewById(R.id.etTxakurra2)
        etTxakurra3 = findViewById(R.id.etTxakurra3)
        etTxakurra4 = findViewById(R.id.etTxakurra4)
        etTxakurra5 = findViewById(R.id.etTxakurra5)
    }

    // ============================================================================
    // ANIMACIONES
    // ============================================================================

    /**
     * Anima los elementos visuales al iniciar la actividad.
     *
     * Secuencia de animaciones:
     * 1. Mascota saluda con animación wave
     * 2. Imagen del perro aparece con bounce
     * 3. Imagen del león aparece con bounce (300ms delay)
     */
    private fun animateInitialElements() {
        // Animar mascota saludando
        val waveAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_wave)
        ivMascota.startAnimation(waveAnim)

        // Animar imágenes de los animales
        val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_bounce_in)

        // Perro aparece primero
        ivPerro.startAnimation(bounceAnim)

        // León aparece 300ms después
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            ivLeon.startAnimation(bounceAnim)
        }, 300)
    }

    /**
     * Cambia la mascota a estado de celebración con animación completa.
     * Se ejecuta cuando el estudiante completa correctamente la tabla.
     */
    private fun animateMascotaCelebracion() {
        // Cambiar imagen a mascota celebrando
        ivMascota.setImageResource(R.drawable.mascota_celebrando)

        // Ejecutar animación de celebración (giro + salto)
        val celebrateAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_celebrate)
        ivMascota.startAnimation(celebrateAnim)
    }

    /**
     * Cambia la mascota a estado triste con animación de negación.
     * Se ejecuta cuando el estudiante intenta finalizar sin completar todos los campos.
     */
    private fun animateMascotaTriste() {
        // Cambiar imagen a mascota triste
        ivMascota.setImageResource(R.drawable.mascota_triste)

        // Ejecutar animación de shake (movimiento de cabeza)
        val shakeAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_shake)
        ivMascota.startAnimation(shakeAnim)

        // Volver a mascota saludando después de 2 segundos
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            ivMascota.setImageResource(R.drawable.mascota_saludando)
        }, 2000)
    }

    // ============================================================================
    // VALIDACIÓN Y LÓGICA DEL EJERCICIO
    // ============================================================================

    /**
     * Configura el botón "Osatu Taula" (Completar Tabla).
     *
     * Funcionamiento:
     * 1. Verifica si ambas columnas están completas
     * 2. Si está completo:
     *    - Anima mascota celebrando
     *    - Muestra mensaje de éxito
     *    - Cierra la actividad después de 2 segundos
     * 3. Si está incompleto:
     *    - Anima mascota triste
     *    - Muestra mensaje específico indicando qué falta
     *    - Mascota vuelve a estado normal después de 2 segundos
     */
    private fun setupFinishButton() {
        btnFinish.setOnClickListener {
            val lehoiaCompleto = checkLehoiaComplete()
            val txakurraCompleto = checkTxakurraComplete()

            if (lehoiaCompleto && txakurraCompleto) {
                // ============ TABLA COMPLETA ============

                animateMascotaCelebracion()

                Toast.makeText(
                    this,
                    "🎉 Bikain! Taula osatu duzu!",
                    Toast.LENGTH_LONG
                ).show()

                // Cerrar actividad después de 2 segundos
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    finish()
                }, 2000)

            } else {
                // ============ TABLA INCOMPLETA ============

                animateMascotaTriste()

                // Mensaje específico según qué falta
                val mensaje = when {
                    !lehoiaCompleto && !txakurraCompleto ->
                        "Mesedez, bete bi zutabeak"  // Por favor, completa ambas columnas
                    !lehoiaCompleto ->
                        "Mesedez, bete Lehoia zutabea"  // Por favor, completa la columna del León
                    else ->
                        "Mesedez, bete Txakurra zutabea"  // Por favor, completa la columna del Perro
                }

                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Verifica si la columna del león (Lehoia) está completa.
     *
     * Criterio: Los 5 campos deben tener al menos un carácter no vacío.
     *
     * @return true si todos los campos del león tienen contenido, false en caso contrario
     */
    private fun checkLehoiaComplete(): Boolean {
        return etLehoia1.text.isNotEmpty() &&
                etLehoia2.text.isNotEmpty() &&
                etLehoia3.text.isNotEmpty() &&
                etLehoia4.text.isNotEmpty() &&
                etLehoia5.text.isNotEmpty()
    }

    /**
     * Verifica si la columna del perro (Txakurra) está completa.
     *
     * Criterio: Los 5 campos deben tener al menos un carácter no vacío.
     *
     * @return true si todos los campos del perro tienen contenido, false en caso contrario
     */
    private fun checkTxakurraComplete(): Boolean {
        return etTxakurra1.text.isNotEmpty() &&
                etTxakurra2.text.isNotEmpty() &&
                etTxakurra3.text.isNotEmpty() &&
                etTxakurra4.text.isNotEmpty() &&
                etTxakurra5.text.isNotEmpty()
    }
}