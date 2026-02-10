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
import com.bumptech.glide.Glide

/**
 * Actividad que representa la parada "Antzinako Harresia" (La Antigua Muralla).
 *
 * Esta actividad se divide en dos fases:
 * 1. **Fase Educativa:** El alumno puede leer un texto o escuchar un audio explicando la historia de la muralla de Bilbao.
 * 2. **Fase de Juego (Quiz):** Un cuestionario de 5 preguntas.
 * * Cada acierto revela una pieza de la imagen de la muralla.
 * * Al finalizar, se guarda la puntuación y se desbloquea el marcador en el mapa.
 *
 * @author Marco
 * @version 1.0
 */
class MurallaActivity : AppCompatActivity() {

    // --- Variables de la Interfaz (UI) ---
    private lateinit var txtIntro1: TextView
    private lateinit var txtIntro2: TextView
    private lateinit var tvLeerMas: TextView
    private lateinit var ivLeon: ImageView

    private lateinit var txtTitulo: TextView
    private lateinit var btnComenzar: Button

    // Controles de Audio
    private lateinit var btnPlayPauseAudio: ImageButton
    private lateinit var seekBarAudio: SeekBar

    // Contenedores del juego
    private lateinit var layoutMuralla: LinearLayout
    private lateinit var listaPiezas: List<ImageView> // Array con las imágenes de las piezas

    // Elementos del Quiz
    private lateinit var txtPregunta: TextView
    private lateinit var grupoOpciones: RadioGroup
    private lateinit var op1: RadioButton
    private lateinit var op2: RadioButton
    private lateinit var op3: RadioButton
    private lateinit var btnResponder: Button

    // Elementos de Resultado
    private lateinit var ivGifResultado: ImageView
    private lateinit var btnFinalizar: Button
    private lateinit var btnVolverMapa: ImageButton

    // --- Variables de Lógica ---
    private var audio: MediaPlayer? = null
    private val audioHandler = Handler(Looper.getMainLooper())
    private var isPlaying = false
    private var indicePregunta = 0
    private var progreso = 0 // Número de aciertos
    private var puntuacionActual = 0
    private var textoDesplegado = false // Controla si el texto "Leer más" está expandido

    /** Runnable para actualizar la barra de progreso del audio cada 0.5s. */
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

    // Datos de las preguntas (Enunciado, Opciones, Índice de la correcta)
    private val preguntas = listOf(
        Pregunta("1. Zer funtzio betetzen zuen harresiak?", listOf("Babesteko.", "Dekoratzeko.", "Turistak erakartzeko."), 0),
        Pregunta("2. Zer aurki zezaketen harresiaren barruan?", listOf("Liburuak.", "Animaliak.", "Etxe, denda eta Katedrala."), 2),
        Pregunta("3. Zer izen hartu zuen kaleak?", listOf("Hondakin kalea.", "Jolas kalea.", "Pilota kalea."), 2),
        Pregunta("4. Zergatik du Erronda kaleak izen hori?", listOf("Guardek errondak egiten zituztelako.", "Ez du definiziorik.", "Herritarrek asmatu zuten."), 0),
        Pregunta("5. Zer gogorarazten digu harresiak?", listOf("Guda garai bat.", "Kutsadura.", "Zaintza garaia."), 2)
    )

    /**
     * Método de inicio de la actividad.
     *
     * Inicializa las vistas, carga los textos, configura el audio y los botones.
     * También aplica los ajustes de accesibilidad si están activados.
     *
     * @param savedInstanceState Estado guardado.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_muralla)

        // --- 1. Inicialización de Vistas ---
        txtTitulo = findViewById(R.id.txtTitulo)
        txtIntro1 = findViewById(R.id.txtIntro1)
        txtIntro2 = findViewById(R.id.txtIntro2)
        tvLeerMas = findViewById(R.id.tvLeerMas)
        ivLeon = findViewById(R.id.ivLeonExplicacion)

        btnComenzar = findViewById(R.id.btnComenzar)
        btnPlayPauseAudio = findViewById(R.id.btnPlayPauseAudio)
        seekBarAudio = findViewById(R.id.seekBarAudio)
        layoutMuralla = findViewById(R.id.layoutMuralla)

        ivGifResultado = findViewById(R.id.ivGifResultado)

        btnFinalizar = findViewById(R.id.btnFinalizar)
        btnVolverMapa = findViewById(R.id.btnVolverMapa)

        // Configuración del botón salir
        btnVolverMapa.visibility = View.VISIBLE
        btnVolverMapa.setOnClickListener {
            if (isPlaying) pauseAudio()
            finish()
        }

        // --- 2. Referencia a las 5 piezas de la muralla ---
        listaPiezas = listOf(
            findViewById(R.id.pieza0),
            findViewById(R.id.pieza1),
            findViewById(R.id.pieza2),
            findViewById(R.id.pieza3),
            findViewById(R.id.pieza4)
        )
        // Al principio están ocultas (invisibles)
        listaPiezas.forEach { it.visibility = View.INVISIBLE }

        // Elementos del cuestionario
        txtPregunta = findViewById(R.id.txtPregunta)
        grupoOpciones = findViewById(R.id.grupoOpciones)
        op1 = findViewById(R.id.op1)
        op2 = findViewById(R.id.op2)
        op3 = findViewById(R.id.op3)
        btnResponder = findViewById(R.id.btnResponder)

        // --- 3. Accesibilidad (Texto Grande) ---
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

        // Estado inicial de botones
        btnComenzar.visibility = View.VISIBLE
        btnFinalizar.visibility = View.GONE

        // --- 4. Asignación de Textos Educativos ---
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

        // Lógica de "Leer más" / "Leer menos"
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

        // --- 5. Preparación de Audio y Juego ---
        mostrarTest(false) // Ocultar el test al inicio
        prepararAudio()

        btnPlayPauseAudio.setOnClickListener { if (isPlaying) pauseAudio() else playAudio() }

        // Control manual de la barra de audio
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

        // Botón para iniciar el cuestionario
        btnComenzar.setOnClickListener {
            btnComenzar.visibility = View.GONE
            btnVolverMapa.visibility = View.GONE // Ocultamos salir para que termine el juego
            mostrarTest(true)
            mostrarPregunta()
            if (isPlaying) pauseAudio() // Parar audio si estaba sonando
        }

        btnResponder.setOnClickListener { comprobarRespuesta() }

        btnFinalizar.setOnClickListener {
            finish()
        }
    }

    /** Inicializa el reproductor de audio con el archivo raw/jarduera_2. */
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

    /**
     * Alterna la visibilidad entre la fase de explicación y la fase de test.
     *
     * @param visible true para mostrar el test, false para mostrar la explicación.
     */
    private fun mostrarTest(visible: Boolean) {
        val vis = if (visible) View.VISIBLE else View.GONE
        layoutMuralla.visibility = vis
        txtPregunta.visibility = vis
        grupoOpciones.visibility = vis
        btnResponder.visibility = vis

        // Elementos a ocultar cuando empieza el test
        txtIntro1.visibility = if (visible) View.GONE else View.VISIBLE
        txtIntro2.visibility = View.GONE
        tvLeerMas.visibility = if (visible) View.GONE else View.VISIBLE
        ivLeon.visibility = if (visible) View.GONE else View.VISIBLE
        btnPlayPauseAudio.visibility = if (visible) View.GONE else View.VISIBLE
        seekBarAudio.visibility = if (visible) View.GONE else View.VISIBLE
    }

    /**
     * Carga la pregunta actual en la interfaz y limpia la selección anterior.
     */
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

    /**
     * Verifica si la opción seleccionada es correcta.
     *
     * * **Acierto:** Suma puntos, incrementa progreso y muestra una pieza de la muralla.
     * * **Fallo:** Resta puntos.
     * * Avanza a la siguiente pregunta o finaliza el juego.
     */
    private fun comprobarRespuesta() {
        // Mapeo del ID del RadioButton al índice (0, 1, 2)
        val seleccion = when (grupoOpciones.checkedRadioButtonId) {
            R.id.op1 -> 0
            R.id.op2 -> 1
            R.id.op3 -> 2
            else -> -1
        }

        if (seleccion == -1) return // No se seleccionó nada

        if (seleccion == preguntas[indicePregunta].correcta) {
            progreso++
            puntuacionActual += 100
            // Mostrar la pieza correspondiente a esta pregunta
            if (indicePregunta < listaPiezas.size) listaPiezas[indicePregunta].visibility = View.VISIBLE
        } else {
            puntuacionActual -= 50
            if (puntuacionActual < 0) puntuacionActual = 0
        }

        indicePregunta++
        if (indicePregunta < preguntas.size) mostrarPregunta() else finalizarJuego()
    }

    /**
     * Muestra la pantalla de resultados al terminar el cuestionario.
     *
     * Decide si mostrar al león feliz (pleno de aciertos) o triste, guarda el progreso
     * y habilita el botón para salir.
     */
    private fun finalizarJuego() {
        btnResponder.isEnabled = false
        btnResponder.visibility = View.GONE
        grupoOpciones.visibility = View.GONE

        // Configuración visual del texto de resultado
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

        // ========================================
        // ✅ MARCAR ACTIVIDAD COMO COMPLETADA EN MEMORIA
        // ========================================
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombreUsuario = prefs.getString("nombre_alumno_actual", "") ?: ""
        prefs.edit().putBoolean("completado_muralla_$nombreUsuario", true).apply()

        // CARGAMOS EL GIF CORRESPONDIENTE
        ivGifResultado.visibility = View.VISIBLE
        try {
            Glide.with(this)
                .asGif()
                .load(gifResId)
                .into(ivGifResultado)
        } catch (e: Exception) {
            ivGifResultado.setImageResource(gifResId)
        }

        btnFinalizar.visibility = View.VISIBLE

        // Guardar y sincronizar
        guardarPuntuacionEnBD(puntuacionActual)
        SyncHelper.subirInmediatamente(this)
    }

    /**
     * Guarda la puntuación en la base de datos local.
     */
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

    /** Clase de datos simple para almacenar la estructura de una pregunta. */
    data class Pregunta(val enunciado: String, val opciones: List<String>, val correcta: Int)
}