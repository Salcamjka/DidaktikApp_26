package com.salca.didaktikapp

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide // Importante: Necesitas la librería Glide

class MurallaActivity : AppCompatActivity() {

    private lateinit var txtIntro1: TextView
    private lateinit var txtIntro2: TextView
    private lateinit var tvLeerMas: TextView
    private lateinit var ivLeon: ImageView

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

    // Referencia al ImageView del GIF
    private lateinit var ivGifResultado: ImageView

    private lateinit var btnFinalizar: Button
    private lateinit var btnVolverMapa: ImageButton

    private var audio: MediaPlayer? = null
    private val audioHandler = Handler(Looper.getMainLooper())
    private var isPlaying = false
    private var indicePregunta = 0
    private var progreso = 0
    private var puntuacionActual = 0
    private var textoDesplegado = false

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

        // Inicialización
        txtTitulo = findViewById(R.id.txtTitulo)
        txtIntro1 = findViewById(R.id.txtIntro1)
        txtIntro2 = findViewById(R.id.txtIntro2)
        tvLeerMas = findViewById(R.id.tvLeerMas)
        ivLeon = findViewById(R.id.ivLeonExplicacion)

        btnComenzar = findViewById(R.id.btnComenzar)
        btnPlayPauseAudio = findViewById(R.id.btnPlayPauseAudio)
        seekBarAudio = findViewById(R.id.seekBarAudio)
        layoutMuralla = findViewById(R.id.layoutMuralla)

        // Referencia al ImageView
        ivGifResultado = findViewById(R.id.ivGifResultado)

        btnFinalizar = findViewById(R.id.btnFinalizar)
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

        val sharedPref = getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
        val usarTextoGrande = sharedPref.getBoolean("MODO_TEXTO_GRANDE", false)
        if (usarTextoGrande) {
            txtTitulo.textSize = 34f
            txtIntro1.textSize = 24f
            txtIntro2.textSize = 24f
            tvLeerMas.textSize = 22f
            txtPregunta.textSize = 22f
            op1.textSize = 20f
            op2.textSize = 20f
            op3.textSize = 20f
            btnComenzar.textSize = 22f
            btnResponder.textSize = 22f
            btnFinalizar.textSize = 22f
        }

        btnComenzar.visibility = View.VISIBLE
        btnFinalizar.visibility = View.GONE

        // Textos
        txtIntro1.text = "Orain dela urte asko, Bilbon harrizko harresi handi bat eraiki zen hiria babesteko asmoarekin.\n" +
                "Bertan familia garrantzitsuenak bizi ziren, euren etxe, denda eta Katedralarekin. Harresitik\n" +
                "kanpo, berriz, herri giroa zegoen, Pelota eta Ronda izeneko kaleetan."

        txtIntro2.text = "\nDenboraren poderioz, hiria hazi egin zen eta harresia ez zen hain beharrezkoa. Zati batzuk,\n" +
                "gainean etxeak eraikitzeko erabili ziren, eta beste batzuk eraitsiz joan ziren, nahiz eta gaur\n" +
                "egun ere egon zela gogorarazten diguten aztarnak dauden. Erronda kalean, esaterako,\n" +
                "harresiaren gainean egindako fatxadak ikus daitezke, eta San Anton elizaren azpian ere\n" +
                "aztarna garrantzitsuak daude.\n" +
                "\n" +
                "Harresiak bere oroitzapena utzi zuen Alde Zaharreko bi kaleren izenean. Pelota kaleari\n" +
                "horrela deitzen zaio jendeak frontoi bat bezala erabiltzen zuelako harresia. Erronda kaleari,\n" +
                "aldiz, harresia zaintzen zuten soldaduek guardiako txandak egiten zituztelako. Horregatik,\n" +
                "gaur egun ere kale honek zaintza garai hura gogorarazten digu."

        tvLeerMas.setOnClickListener {
            if (!textoDesplegado) {
                txtIntro2.visibility = View.VISIBLE
                tvLeerMas.text = "Irakurri gutxiago ▲"
                ivLeon.visibility = View.GONE
                textoDesplegado = true
            } else {
                txtIntro2.visibility = View.GONE
                tvLeerMas.text = "Irakurri gehiago ▼"
                ivLeon.visibility = View.VISIBLE
                textoDesplegado = false
            }
        }

        mostrarTest(false)
        prepararAudio()

        btnPlayPauseAudio.setOnClickListener { if (isPlaying) pauseAudio() else playAudio() }

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

        btnFinalizar.setOnClickListener {
            finish()
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
        } catch (e: Exception) { }
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

        txtIntro1.visibility = if (visible) View.GONE else View.VISIBLE
        txtIntro2.visibility = View.GONE
        tvLeerMas.visibility = if (visible) View.GONE else View.VISIBLE
        ivLeon.visibility = if (visible) View.GONE else View.VISIBLE
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

        if (seleccion == -1) return

        if (seleccion == preguntas[indicePregunta].correcta) {
            progreso++
            puntuacionActual += 100
            if (indicePregunta < listaPiezas.size) listaPiezas[indicePregunta].visibility = View.VISIBLE
        } else {
            puntuacionActual -= 50
            if (puntuacionActual < 0) puntuacionActual = 0
        }

        indicePregunta++
        if (indicePregunta < preguntas.size) mostrarPregunta() else finalizarJuego()
    }

    private fun finalizarJuego() {
        btnResponder.isEnabled = false
        btnResponder.visibility = View.GONE
        grupoOpciones.visibility = View.GONE

        txtPregunta.gravity = Gravity.CENTER
        val sharedPref = getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
        val usarTextoGrande = sharedPref.getBoolean("MODO_TEXTO_GRANDE", false)
        if(usarTextoGrande) txtPregunta.textSize = 30f else txtPregunta.textSize = 24f

        txtPregunta.visibility = View.VISIBLE
        txtPregunta.setTypeface(null, android.graphics.Typeface.BOLD)

        // VARIABLE PARA DECIDIR QUÉ GIF MOSTRAR
        val gifResId: Int

        if (progreso == preguntas.size) {
            // SI ACIERTA TODAS
            txtPregunta.text = "🏰 Zorionak!\nHarresia osatu duzu!"
            txtPregunta.setTextColor(ContextCompat.getColor(this, R.color.mi_acierto))
            gifResId = R.drawable.leonfeliz // León feliz
        } else {
            // SI FALLA ALGUNA
            txtPregunta.text = "Galdu duzu!\n(Puntuazioa: $progreso/5)"
            txtPregunta.setTextColor(ContextCompat.getColor(this, R.color.mi_error_texto))
            gifResId = R.drawable.leontriste // León triste
        }

        // CARGAMOS EL GIF SELECCIONADO
        ivGifResultado.visibility = View.VISIBLE
        try {
            Glide.with(this)
                .asGif()
                .load(gifResId) // Carga el ID decidido arriba
                .into(ivGifResultado)
        } catch (e: Exception) {
            ivGifResultado.setImageResource(gifResId)
        }

        btnFinalizar.visibility = View.VISIBLE
        guardarPuntuacionEnBD(puntuacionActual)
        SyncHelper.subirInmediatamente(this)
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