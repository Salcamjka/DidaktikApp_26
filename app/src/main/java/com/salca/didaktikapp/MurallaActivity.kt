package com.salca.didaktikapp

import android.content.Context
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
    private var progreso = 0
    private var puntuacionActual = 0

    // Runnable para actualizar la barra
    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            try {
                if (audio != null && isPlaying) {
                    seekBarAudio.progress = audio!!.currentPosition
                }
                // Se repite cada 500ms
                audioHandler.postDelayed(this, 500)
            } catch (e: Exception) { }
        }
    }

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

        // 1. PREPARAMOS EL AUDIO AL INICIO (para que la barra sepa la duración)
        prepararAudio()

        // 2. CONTROL DEL BOTÓN PLAY/PAUSE
        btnPlayPauseAudio.setOnClickListener {
            if (isPlaying) {
                pauseAudio()
            } else {
                playAudio()
            }
        }

        // 3. BARRA DE PROGRESO (CONTROL MANUAL)
        seekBarAudio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Si el usuario mueve la barra, actualizamos el audio inmediatamente
                if (fromUser) audio?.seekTo(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // AL TOCAR: Paramos la actualización automática para que no "tiemble"
                audioHandler.removeCallbacks(updateSeekBarRunnable)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // AL SOLTAR: Volvemos a activar la actualización automática si está sonando
                if (isPlaying) {
                    audioHandler.postDelayed(updateSeekBarRunnable, 500)
                }
            }
        })

        btnComenzar.setOnClickListener {
            mostrarTest(true)
            mostrarPregunta()
            if (isPlaying) pauseAudio()
        }

        btnResponder.setOnClickListener { comprobarRespuesta() }
        btnReintentar.setOnClickListener { reintentar() }
    }

    private fun prepararAudio() {
        try {
            if (audio == null) {
                audio = MediaPlayer.create(this, R.raw.jarduera_2)
                audio?.setOnCompletionListener {
                    pauseAudio()
                    audio?.seekTo(0)
                    seekBarAudio.progress = 0
                    btnComenzar.visibility = View.VISIBLE
                }
                // Configuramos el máximo de la barra
                seekBarAudio.max = audio?.duration ?: 0
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Errorea audioarekin", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playAudio() {
        audio?.start()
        isPlaying = true
        btnPlayPauseAudio.setImageResource(android.R.drawable.ic_media_pause)
        audioHandler.post(updateSeekBarRunnable)
    }

    private fun pauseAudio() {
        audio?.pause()
        isPlaying = false
        btnPlayPauseAudio.setImageResource(android.R.drawable.ic_media_play)
        audioHandler.removeCallbacks(updateSeekBarRunnable)
    }

    private fun mostrarTest(visible: Boolean) {
        val vis = if (visible) View.VISIBLE else View.GONE
        layoutMuralla.visibility = vis
        txtPregunta.visibility = vis
        grupoOpciones.visibility = vis
        btnResponder.visibility = vis
        txtIntro.visibility = if (visible) View.GONE else View.VISIBLE
        btnPlayPauseAudio.visibility = if (visible) View.GONE else View.VISIBLE
        seekBarAudio.visibility = if (visible) View.GONE else View.VISIBLE
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

        if (seleccion == preguntas[indicePregunta].correcta) {
            progreso++
            puntuacionActual += 100

            if (indicePregunta < listaPiezas.size) listaPiezas[indicePregunta].visibility = View.VISIBLE
            Toast.makeText(this, "Zuzena! (+100 pts)", Toast.LENGTH_SHORT).show()
        } else {
            puntuacionActual -= 50
            if (puntuacionActual < 0) puntuacionActual = 0
            Toast.makeText(this, "Ez da zuzena (-50 pts)", Toast.LENGTH_SHORT).show()
        }

        indicePregunta++
        if (indicePregunta < preguntas.size) mostrarPregunta() else finalizarJuego()
    }

    private fun finalizarJuego() {
        btnResponder.isEnabled = false
        if (progreso == preguntas.size) {
            txtPregunta.text = "🏰 Zorionak! Harresia osatu da!\nPuntuazioa: $puntuacionActual"
        } else {
            txtPregunta.text = "Amaiera!\nPuntuazioa: $puntuacionActual"
            btnReintentar.visibility = View.VISIBLE
        }
        guardarPuntuacionEnBD(puntuacionActual)
    }

    private fun guardarPuntuacionEnBD(puntos: Int) {
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombreAlumno = prefs.getString("nombre_alumno_actual", "Anonimo") ?: "Anonimo"

        val dbHelper = DatabaseHelper(this)
        val guardado = dbHelper.guardarPuntuacion(nombreAlumno, "Muralla", puntos)
    }

    private fun reintentar() {
        indicePregunta = 0
        progreso = 0
        puntuacionActual = 0
        btnResponder.isEnabled = true
        btnReintentar.visibility = View.GONE
        listaPiezas.forEach { it.visibility = View.INVISIBLE }
        mostrarPregunta()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioHandler.removeCallbacks(updateSeekBarRunnable)
        audio?.release()
        audio = null
    }

    data class Pregunta(val enunciado: String, val opciones: List<String>, val correcta: Int)
}