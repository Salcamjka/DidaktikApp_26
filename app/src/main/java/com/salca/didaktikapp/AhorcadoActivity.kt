package com.dm2.ahorcadodidaktikapp

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AhorcadoActivity : AppCompatActivity() {

    // --- Variables Pantalla Juego ---
    private lateinit var layoutJuego: LinearLayout
    private lateinit var ivAhorcado: ImageView
    private lateinit var tvPalabra: TextView
    private lateinit var llTeclado: LinearLayout

    // --- Variables Pantalla Final ---
    private lateinit var layoutFinal: LinearLayout
    private lateinit var ivFinalSuperior: ImageView
    private lateinit var tvMensajePrincipal: TextView
    private lateinit var tvMensajeSecundario: TextView
    private lateinit var ivFinalInferior: ImageView
    private lateinit var btnJarraitu: Button

    // --- Variables Lógicas ---
    private val palabras = listOf("ATHLETIC-EN ARMARRIA")
    private var palabraActual = ""
    private var letrasAdivinadas = mutableListOf<Char>()
    private var errores = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Vincular vistas
        layoutJuego = findViewById(R.id.layoutJuego)
        ivAhorcado = findViewById(R.id.ivAhorcado)
        tvPalabra = findViewById(R.id.tvPalabra)
        llTeclado = findViewById(R.id.llTeclado)

        layoutFinal = findViewById(R.id.layoutFinal)
        ivFinalSuperior = findViewById(R.id.ivFinalSuperior)
        tvMensajePrincipal = findViewById(R.id.tvMensajePrincipal)
        tvMensajeSecundario = findViewById(R.id.tvMensajeSecundario)
        ivFinalInferior = findViewById(R.id.ivFinalInferior)
        btnJarraitu = findViewById(R.id.btnJarraitu)

        // Configurar botón
        btnJarraitu.setOnClickListener {
            val intent = android.content.Intent(this, AhorcadoTestuaActivity::class.java)
            startActivity(intent)
            finish() // Opcional: cierra el ahorcado para que no puedan volver atrás con el botón 'back'
        }

// Arrancar
        iniciarJuego()
    }

    private fun iniciarJuego() {
        errores = 0
        letrasAdivinadas.clear()

        palabraActual = palabras.random()

        layoutFinal.visibility = View.GONE
        layoutJuego.visibility = View.VISIBLE

        ivAhorcado.setImageResource(R.drawable.ahorcado0)

        crearTeclado()
        actualizarTextoPantalla()
    }

    private fun crearTeclado() {
        llTeclado.removeAllViews()

        var contador = 0
        var filaActual: LinearLayout? = null

        for (letra in 'A'..'Z') {
            if (contador % 7 == 0) {
                filaActual = LinearLayout(this)
                filaActual.orientation = LinearLayout.HORIZONTAL
                filaActual.gravity = Gravity.CENTER

                val paramsFila = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                paramsFila.setMargins(0, 5, 0, 5)
                filaActual.layoutParams = paramsFila

                llTeclado.addView(filaActual)
            }

            val boton = Button(this)
            boton.text = letra.toString()
            boton.textSize = 14f

            val paramsBoton = LinearLayout.LayoutParams(110, 110)
            paramsBoton.setMargins(5, 0, 5, 0)
            boton.layoutParams = paramsBoton

            boton.setOnClickListener { procesarLetra(letra, boton) }
            filaActual?.addView(boton)

            contador++
        }
    }

    private fun procesarLetra(letra: Char, boton: Button) {
        if (palabraActual.contains(letra)) {
            // ACIERTO
            if (!letrasAdivinadas.contains(letra)) {
                letrasAdivinadas.add(letra)
            }
            boton.isEnabled = false
            boton.setBackgroundColor(Color.parseColor("#4CAF50"))

            actualizarTextoPantalla()
            verificarVictoria()
        } else {
            // FALLO
            errores++
            actualizarImagenAhorcado()

            // CAMBIO: Ahora perdemos al llegar a 7, no a 6.
            // Así, cuando llegas a 6, ves el muñeco completo y tienes una oportunidad más.
            if (errores >= 7) {
                mostrarPantallaFinal(gano = false)
            }
        }
    }

    private fun actualizarTextoPantalla() {
        val sb = StringBuilder()
        for (c in palabraActual) {
            if (c == ' ') {
                sb.append("\n")
            } else {
                if (letrasAdivinadas.contains(c) || c == '-') {
                    sb.append("$c ")
                } else {
                    sb.append("_ ")
                }
            }
        }
        tvPalabra.text = sb.toString()
    }

    private fun verificarVictoria() {
        var gano = true
        for (c in palabraActual) {
            if (c != '-' && c != ' ') {
                if (!letrasAdivinadas.contains(c)) {
                    gano = false
                    break
                }
            }
        }
        if (gano) {
            mostrarPantallaFinal(gano = true)
        }
    }

    private fun actualizarImagenAhorcado() {
        val res = when (errores) {
            0 -> R.drawable.ahorcado0
            1 -> R.drawable.ahorcado1
            2 -> R.drawable.ahorcado2
            3 -> R.drawable.ahorcado3
            4 -> R.drawable.ahorcado4
            5 -> R.drawable.ahorcado5
            // CAMBIO: Tanto el 6 como el 7 muestran el dibujo completo (ahorcado6)
            else -> R.drawable.ahorcado6
        }
        ivAhorcado.setImageResource(res)
    }

    private fun mostrarPantallaFinal(gano: Boolean) {
        layoutJuego.visibility = View.GONE
        layoutFinal.visibility = View.VISIBLE

        if (gano) {
            ivFinalSuperior.visibility = View.VISIBLE
            ivFinalSuperior.setImageResource(R.drawable.escudo)

            tvMensajePrincipal.text = palabraActual
            tvMensajePrincipal.setTextColor(Color.BLACK)

            tvMensajeSecundario.visibility = View.VISIBLE
            tvMensajeSecundario.text = "Oso ondo, irabazi duzu!"

            ivFinalInferior.setImageResource(R.drawable.leonfeliz)
        } else {
            ivFinalSuperior.visibility = View.GONE
            tvMensajeSecundario.visibility = View.GONE

            tvMensajePrincipal.text = "Galdu duzu.\nHitza hau zen:\n$palabraActual"
            tvMensajePrincipal.setTextColor(Color.RED)

            ivFinalInferior.setImageResource(R.drawable.leontriste)
        }
    }
}