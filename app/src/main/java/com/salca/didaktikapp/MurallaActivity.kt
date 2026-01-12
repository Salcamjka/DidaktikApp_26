package com.salca.didaktikapp

import android.content.Context // Import necesario para guardar
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MurallaActivity : AppCompatActivity() {

    private lateinit var txtIntro: TextView
    private lateinit var txtTitulo: TextView
    private lateinit var btnComenzar: Button
    private lateinit var btnPlayPauseAudio: ImageButton
    private lateinit var seekBarAudio: SeekBar
    private lateinit var layoutMuralla: LinearLayout
    private lateinit var listaPiezas: List<ImageView>
    private lateinit var txtPregunta: TextView
    private lateinit var grupoOpciones: RadioGroup
    private lateinit var op1: RadioButton
    private lateinit var op2: RadioButton
    private lateinit var op3: RadioButton
    private lateinit var btnResponder: Button
    private lateinit var btnReintentar: Button

    private var audio: MediaPlayer? = null
    private val audioHandler = Handler(Looper.getMainLooper())
    private var isPlaying = false
    private var indicePregunta = 0
    private var progreso = 0 // Piezas conseguidas

    // --- NUEVA VARIABLE DE PUNTUACIÓN ---
    private var puntuacionActual = 0

    private val preguntas = listOf(
        Pregunta("1. Zer funtzio betetzen zuen harresiak?", listOf("Babesteko.", "Dekoratzeko.", "Turistak erakartzeko."), 0),
        Pregunta("2. Zer aurki zezaketen harresiaren barruan?", listOf("Liburuak.", "Animaliak.", "Etxe, denda eta Katedrala."), 2),
        Pregunta("3. Zer izen hartu zuen kaleak?", listOf("Hondakin kalea.", "Jolas kalea.", "Pilota kalea."), 2),
        Pregunta("4. Zergatik du Erronda kaleak izen hori?", listOf("Guardek errondak egiten zituztelako.", "Ez du definiziorik.", "Herritarrek asmatu zuten."), 0),
        Pregunta("5. Zer gogorarazten digu harresiak?", listOf("Guda garai bat.", "Kutsadura.", "Zaintza garaia."), 2)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_muralla)

        // Inicialización
        txtTitulo = findViewById(R.id.txtTitulo)
        txtIntro = findViewById(R.id.txtIntro)
        btnComenzar = findViewById(R.id.btnComenzar)
        btnPlayPauseAudio = findViewById(R.id.btnPlayPauseAudio)
        seekBarAudio = findViewById(R.id.seekBarAudio)
        layoutMuralla = findViewById(R.id.layoutMuralla)

        listaPiezas = listOf(
            findViewById(R.id.pieza0),
            findViewById(R.id.pieza1),
            findViewById(R.id.pieza2),
            findViewById(R.id.pieza3),
            findViewById(R.id.pieza4)
        )

        listaPiezas.forEach { it.visibility = View.INVISIBLE }

        txtPregunta = findViewById(R.id.txtPregunta)
        grupoOpciones = findViewById(R.id.grupoOpciones)
        op1 = findViewById(R.id.op1)
        op2 = findViewById(R.id.op2)
        op3 = findViewById(R.id.op3)
        btnResponder = findViewById(R.id.btnResponder)
        btnReintentar = findViewById(R.id.btnReintentar)

        btnComenzar.visibility = View.GONE

        txtIntro.text = "Orain dela urte asko, Bilbon harrizko harresi handi bat eraiki zen hiria babesteko asmoarekin..."

        mostrarTest(false)

        btnPlayPauseAudio.setOnClickListener {
            if (audio == null) {
                audio = MediaPlayer.create(this, R.raw.jarduera_2)
                audio?.setOnCompletionListener {
                    btnComenzar.visibility = View.VISIBLE
                    btnPlayPauseAudio.setImageResource(android.R.drawable.ic_media_play)
                    isPlaying = false
                }
                seekBarAudio.max = audio?.duration ?: 0
                actualizarSeekBar()
            }
            if (isPlaying) {
                audio?.pause()
                btnPlayPauseAudio.setImageResource(android.R.drawable.ic_media_play)
            } else {
                audio?.start()
                btnPlayPauseAudio.setImageResource(android.R.drawable.ic_media_pause)
            }
            isPlaying = !isPlaying
        }

        btnComenzar.setOnClickListener {
            mostrarTest(true)
            mostrarPregunta()
        }

        btnResponder.setOnClickListener { comprobarRespuesta() }
        btnReintentar.setOnClickListener { reintentar() }
    }

    private fun actualizarSeekBar() {
        val runnable = object : Runnable {
            override fun run() {
                audio?.let { seekBarAudio.progress = it.currentPosition }
                audioHandler.postDelayed(this, 500)
            }
        }
        audioHandler.post(runnable)
    }

    private fun mostrarTest(visible: Boolean) {
        val vis = if (visible) View.VISIBLE else View.GONE
        layoutMuralla.visibility = vis
        txtPregunta.visibility = vis
        grupoOpciones.visibility = vis
        btnResponder.visibility = vis
        txtIntro.visibility = if (visible) View.GONE else View.VISIBLE
        btnPlayPauseAudio.visibility = if (visible) View.GONE else View.VISIBLE
    }

    private fun mostrarPregunta() {
        if (indicePregunta < preguntas.size) {
            val p = preguntas[indicePregunta]
            txtPregunta.text = p.enunciado
            op1.text = p.opciones[0]
            op2.text = p.opciones[1]
            op3.text = p.opciones[2]
            grupoOpciones.clearCheck()
        }
    }

    private fun comprobarRespuesta() {
        val seleccion = when (grupoOpciones.checkedRadioButtonId) {
            R.id.op1 -> 0
            R.id.op2 -> 1
            R.id.op3 -> 2
            else -> -1
        }

        if (seleccion == -1) {
            Toast.makeText(this, "Aukeratu erantzun bat", Toast.LENGTH_SHORT).show()
            return
        }

        // --- LÓGICA DE PUNTUACIÓN AÑADIDA ---
        if (seleccion == preguntas[indicePregunta].correcta) {
            progreso++ // Para mostrar la pieza visual
            puntuacionActual += 100 // SUMA 100 PUNTOS

            if (indicePregunta < listaPiezas.size) listaPiezas[indicePregunta].visibility = View.VISIBLE
            Toast.makeText(this, "Zuzena! (+100 pts)", Toast.LENGTH_SHORT).show()
        } else {
            puntuacionActual -= 50 // RESTA 50 PUNTOS
            // PROTECCIÓN: No bajar de 0
            if (puntuacionActual < 0) puntuacionActual = 0

            Toast.makeText(this, "Ez da zuzena (-50 pts)", Toast.LENGTH_SHORT).show()
        }

        indicePregunta++
        if (indicePregunta < preguntas.size) mostrarPregunta() else finalizarJuego()
    }

    private fun finalizarJuego() {
        btnResponder.isEnabled = false

        // Mensaje final con los puntos
        if (progreso == preguntas.size) {
            txtPregunta.text = "🏰 Zorionak! Harresia osatu da!\nPuntuazioa: $puntuacionActual"
        } else {
            txtPregunta.text = "Amaiera!\nPuntuazioa: $puntuacionActual"
            btnReintentar.visibility = View.VISIBLE
        }

        // --- GUARDAR EN BASE DE DATOS ---
        guardarPuntuacionEnBD(puntuacionActual)
    }

    // --- FUNCIÓN PARA GUARDAR EN LA COLUMNA "muralla" ---
    private fun guardarPuntuacionEnBD(puntos: Int) {
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombreAlumno = prefs.getString("nombre_alumno_actual", "Anonimo") ?: "Anonimo"

        val dbHelper = DatabaseHelper(this)

        // Enviamos "Muralla" para que DatabaseHelper lo meta en la columna 'muralla'
        val guardado = dbHelper.guardarPuntuacion(nombreAlumno, "Muralla", puntos)

        if (guardado) {
            Toast.makeText(this, "Gorde da: $nombreAlumno (Muralla: $puntos)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun reintentar() {
        indicePregunta = 0
        progreso = 0
        puntuacionActual = 0 // REINICIAMOS PUNTOS AL REINTENTAR

        btnResponder.isEnabled = true
        btnReintentar.visibility = View.GONE
        listaPiezas.forEach { it.visibility = View.INVISIBLE }
        mostrarPregunta()
    }

    override fun onDestroy() {
        super.onDestroy()
        audio?.release()
        audioHandler.removeCallbacksAndMessages(null)
    }

    data class Pregunta(val enunciado: String, val opciones: List<String>, val correcta: Int)
}