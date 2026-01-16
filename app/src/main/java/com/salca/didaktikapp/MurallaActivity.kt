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

    // NUEVO: Botón Finalizar (Jarraitu)
    private lateinit var btnFinalizar: Button

    private lateinit var btnVolverMapa: ImageButton

    private var audio: MediaPlayer? = null
    private val audioHandler = Handler(Looper.getMainLooper())
    private var isPlaying = false
    private var indicePregunta = 0
    private var progreso = 0
    private var puntuacionActual = 0

    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            try {
                if (audio != null && isPlaying) {
                    seekBarAudio.progress = audio!!.currentPosition
                }
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

        // Inicialización de Vistas
        txtTitulo = findViewById(R.id.txtTitulo)
        txtIntro = findViewById(R.id.txtIntro)
        btnComenzar = findViewById(R.id.btnComenzar)
        btnPlayPauseAudio = findViewById(R.id.btnPlayPauseAudio)
        seekBarAudio = findViewById(R.id.seekBarAudio)
        layoutMuralla = findViewById(R.id.layoutMuralla)

        // Inicializamos el botón finalizar
        btnFinalizar = findViewById(R.id.btnFinalizar)

        // Botón Mapa
        btnVolverMapa = findViewById(R.id.btnVolverMapa)
        btnVolverMapa.visibility = View.VISIBLE
        btnVolverMapa.setOnClickListener {
            if (isPlaying) pauseAudio()
            finish()
        }

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

        btnComenzar.visibility = View.VISIBLE
        btnFinalizar.visibility = View.GONE // Aseguramos que esté oculto

        txtIntro.text = "Orain dela urte asko, Bilbon harrizko harresi handi bat eraiki zen hiria babesteko asmoarekin.\n" +
                "Bertan familia garrantzitsuenak bizi ziren, euren etxe, denda eta Katedralarekin. Harresitik\n" +
                "kanpo, berriz, herri giroa zegoen, Pelota eta Ronda izeneko kaleetan.\n" +
                "\n" +
                "Denboraren poderioz, hiria hazi egin zen eta harresia ez zen hain beharrezkoa. Zati batzuk,\n" +
                "gainean etxeak eraikitzeko erabili ziren, eta beste batzuk eraitsiz joan ziren, nahiz eta gaur\n" +
                "egun ere egon zela gogorarazten diguten aztarnak dauden. Erronda kalean, esaterako,\n" +
                "harresiaren gainean egindako fatxadak ikus daitezke, eta San Anton elizaren azpian ere\n" +
                "aztarna garrantzitsuak daude.\n" +
                "\n" +
                "Harresiak bere oroitzapena utzi zuen Alde Zaharreko bi kaleren izenean. Pelota kaleari\n" +
                "horrela deitzen zaio jendeak frontoi bat bezala erabiltzen zuelako harresia. Erronda kaleari,\n" +
                "aldiz, harresia zaintzen zuten soldaduek guardiako txandak egiten zituztelako. Horregatik,\n" +
                "gaur egun ere kale honek zaintza garai hura gogorarazten digu."

        mostrarTest(false)
        prepararAudio()

        btnPlayPauseAudio.setOnClickListener {
            if (isPlaying) pauseAudio() else playAudio()
        }

        seekBarAudio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) audio?.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                audioHandler.removeCallbacks(updateSeekBarRunnable)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (isPlaying) audioHandler.postDelayed(updateSeekBarRunnable, 500)
            }
        })

        btnComenzar.setOnClickListener {
            btnComenzar.visibility = View.GONE
            btnVolverMapa.visibility = View.GONE
            mostrarTest(true)
            mostrarPregunta()
            if (isPlaying) pauseAudio()
        }

        btnResponder.setOnClickListener { comprobarRespuesta() }

        // Listener para el botón final Jarraitu
        btnFinalizar.setOnClickListener {
            finish() // Volver al mapa
        }
    }

    private fun prepararAudio() {
        try {
            if (audio == null) {
                audio = MediaPlayer.create(this, R.raw.jarduera_2)
                audio?.setOnCompletionListener {
                    pauseAudio()
                    audio?.seekTo(0)
                    seekBarAudio.progress = 0
                }
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
        // Desactivamos el botón de responder y lo ocultamos para dar paso al nuevo botón
        btnResponder.isEnabled = false
        btnResponder.visibility = View.GONE

        if (progreso == preguntas.size) {
            txtPregunta.text = "🏰 Zorionak! Harresia osatu da!\nPuntuazioa: $puntuacionActual"
        } else {
            txtPregunta.text = "Jokoa amaitu da.\nPuntuazioa: $puntuacionActual"
        }

        // MOSTRAR BOTÓN JARRAITU
        btnFinalizar.visibility = View.VISIBLE

        guardarPuntuacionEnBD(puntuacionActual)
    }

    private fun guardarPuntuacionEnBD(puntos: Int) {
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombreAlumno = prefs.getString("nombre_alumno_actual", "Anonimo") ?: "Anonimo"
        val dbHelper = DatabaseHelper(this)
        dbHelper.guardarPuntuacion(nombreAlumno, "Muralla", puntos)
    }

    override fun onDestroy() {
        super.onDestroy()
        audioHandler.removeCallbacks(updateSeekBarRunnable)
        audio?.release()
        audio = null
    }

    data class Pregunta(val enunciado: String, val opciones: List<String>, val correcta: Int)
}