package com.salca.didaktikapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DetalleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle)

        // Obtener el nombre del lugar desde el Intent
        val nombreLugar = intent.getStringExtra("NOMBRE_LUGAR") ?: "Lugar desconocido"

        // Mostrar información del lugar
        val tvTitulo = findViewById<TextView>(R.id.tvTitulo)
        val tvDescripcion = findViewById<TextView>(R.id.tvDescripcion)

        tvTitulo.text = nombreLugar
        tvDescripcion.text = obtenerDescripcion(nombreLugar)
    }

    private fun obtenerDescripcion(lugar: String): String {
        return when (lugar) {
            "Plaza Nueva" -> "La Plaza Nueva es una plaza porticada del siglo XIX situada en el Casco Viejo de Bilbao. Es conocida por sus bares de pintxos y su ambiente festivo los domingos."

            "Catedral de Santiago" -> "La Catedral de Santiago es un templo católico de estilo gótico construido en el siglo XIV. Es uno de los monumentos más emblemáticos del Casco Viejo."

            "Teatro Arriaga" -> "El Teatro Arriaga es un teatro de ópera y ballet inaugurado en 1890. Su arquitectura neobarroca lo convierte en uno de los edificios más bellos de Bilbao."

            "Mercado de la Ribera" -> "El Mercado de la Ribera es el mercado cubierto más grande de Europa. Fue inaugurado en 1929 y ofrece productos frescos locales."

            "Las Siete Calles" -> "Las Siete Calles son el corazón del Casco Viejo de Bilbao. Estas calles medievales forman el núcleo histórico original de la ciudad fundada en 1300."

            else -> "Información no disponible."
        }
    }
}