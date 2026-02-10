package com.salca.didaktikapp

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

/**
 * Actividad encargada de la configuración de Accesibilidad de la aplicación.
 *
 * Esta pantalla permite al usuario activar o desactivar opciones visuales para mejorar la legibilidad:
 * * **Modo Alto Contraste:** Cambia la visualización del mapa a modo híbrido (satélite).
 * * **Texto Grande:** Aumenta el tamaño de la fuente en las actividades de los juegos y explicaciones.
 *
 * Los cambios se guardan de forma persistente utilizando [SharedPreferences] en el archivo "AjustesApp".
 *
 * @author Nizam
 * @version 1.0
 */
class AccesibilidadActivity : AppCompatActivity() {

    /**
     * Método de creación de la actividad.
     *
     * Se encarga de inicializar la interfaz y recuperar las preferencias guardadas anteriormente.
     *
     * @param savedInstanceState Estado anterior de la actividad, si existe.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accesibilidad)

        // --- 1. Referencias a los elementos visuales (Vistas) ---
        val switchContraste = findViewById<Switch>(R.id.switchContraste)
        val switchTexto = findViewById<Switch>(R.id.switchTexto)
        val btnVolver = findViewById<Button>(R.id.btnVolver)

        // --- 2. Preparar el sistema de guardado (SharedPreferences) ---
        // Abrimos el archivo "AjustesApp" en modo privado para que solo esta app pueda leerlo
        val sharedPref = getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        // --- 3. Leer el estado actual ---
        // Recuperamos cómo estaban los interruptores la última vez.
        // Si es la primera vez (no existe el dato), por defecto será 'false' (desactivado).
        switchContraste.isChecked = sharedPref.getBoolean("MODO_OSCURO", false)
        switchTexto.isChecked = sharedPref.getBoolean("MODO_TEXTO_GRANDE", false)

        // --- 4. Listener para el Modo Oscuro/Contraste ---
        // Se ejecuta cada vez que el usuario toca el interruptor de contraste
        switchContraste.setOnCheckedChangeListener { _, isChecked ->
            // Guardamos el nuevo valor (true o false) en la memoria del teléfono
            editor.putBoolean("MODO_OSCURO", isChecked)
            editor.apply() // Confirmamos el guardado
        }

        // --- 5. Listener para el Texto Grande ---
        // Se ejecuta cada vez que el usuario toca el interruptor de tamaño de letra
        switchTexto.setOnCheckedChangeListener { _, isChecked ->
            // Guardamos el nuevo valor en la memoria
            editor.putBoolean("MODO_TEXTO_GRANDE", isChecked)
            editor.apply() // Confirmamos el guardado
        }

        // --- 6. Botón para salir ---
        // Cierra esta actividad y vuelve a la anterior (Mapa)
        btnVolver.setOnClickListener {
            finish()
        }
    }
}