package com.salca.didaktikapp

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SopaActivity : AppCompatActivity() {

    private lateinit var scrollTextContainer: ScrollView
    private lateinit var tvTextoIntroductorio: TextView
    private lateinit var btnComenzarSopa: Button
    private lateinit var ivMascotaPantalla1: ImageView

    // --- CAMBIO: NUEVOS CONTROLES DE AUDIO (Icono + Barra) ---
    private lateinit var btnPlayPauseIcon: ImageButton
    private lateinit var seekBarAudio: SeekBar
    private lateinit var runnable: Runnable
    private var handler = Handler(Looper.getMainLooper())
    // ---------------------------------------------------------

    private lateinit var sopaContainer: ScrollView
    // Asegúrate de que esta clase existe en tu proyecto (WordSearchView)
    private lateinit var wordSearchView: WordSearchView
    private lateinit var tvProgress: TextView
    private lateinit var btnFinish: Button
    private lateinit var ivMascotaPantalla2: ImageView

    private lateinit var cbBarrencalle: CheckBox
    private lateinit var cbBelosticalle: CheckBox
    private lateinit var cbCarniceriaVieja: CheckBox
    private lateinit var cbSomera: CheckBox
    private lateinit var cbArtecalle: CheckBox
    private lateinit var cbTenderia: CheckBox
    private lateinit var cbBarrenkaleBarrena: CheckBox

    private val wordToCheckbox = mutableMapOf<String, CheckBox>()
    private var foundWordsCount = 0
    private val totalWords = 7

    // --- VARIABLE DE PUNTUACIÓN ---
    private var puntuacionActual = 0

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sopa)

        try {
            initializeViews()
            setupAudioControls() // Configurar el nuevo reproductor
            setupWordSearchView()
            setupFinishButton()

            // Inicializar audio para la barra de progreso
            setupAudio()

            mostrarPantallaTexto()
            animateMascotaInicial()
        } catch (e: Exception) {
            Toast.makeText(this, "Errorea Sopa jokoan: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::runnable.isInitialized) handler.removeCallbacks(runnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun initializeViews() {
        scrollTextContainer = findViewById(R.id.scrollTextContainer)
        tvTextoIntroductorio = findViewById(R.id.tvTextoIntroductorio)
        btnComenzarSopa = findViewById(R.id.btnComenzarSopa)
        ivMascotaPantalla1 = findViewById(R.id.ivMascotaPantalla1)

        // --- CAMBIO: Referencias a los nuevos controles ---
        btnPlayPauseIcon = findViewById(R.id.btnPlayPauseIcon)
        seekBarAudio = findViewById(R.id.seekBarAudio)
        // --------------------------------------------------

        sopaContainer = findViewById(R.id.sopaContainer)
        wordSearchView = findViewById(R.id.wordSearchView)
        tvProgress = findViewById(R.id.tvProgress)
        btnFinish = findViewById(R.id.btnFinish)
        ivMascotaPantalla2 = findViewById(R.id.ivMascotaPantalla2)

        cbBarrencalle = findViewById(R.id.cbBarrencalle)
        cbBelosticalle = findViewById(R.id.cbBelosticalle)
        cbCarniceriaVieja = findViewById(R.id.cbCarniceriaVieja)
        cbSomera = findViewById(R.id.cbSomera)
        cbArtecalle = findViewById(R.id.cbArtecalle)
        cbTenderia = findViewById(R.id.cbTenderia)
        cbBarrenkaleBarrena = findViewById(R.id.cbBarrenkaleBarrena)

        wordToCheckbox["SOMERA"] = cbSomera
        wordToCheckbox["ARTEKALE"] = cbArtecalle
        wordToCheckbox["TENDERIA"] = cbTenderia
        wordToCheckbox["BELOSTIKALE"] = cbBelosticalle
        wordToCheckbox["CARNICERIAVIEJA"] = cbCarniceriaVieja
        wordToCheckbox["BARRENKALE"] = cbBarrencalle
        wordToCheckbox["BARRENKALEBARRENA"] = cbBarrenkaleBarrena

        updateProgress()

        btnComenzarSopa.setOnClickListener { mostrarSopaDeLetras() }
    }

    // --- LÓGICA DE AUDIO MEJORADA (CON BARRA) ---
    private fun setupAudio() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.jarduera_3)

            // Configurar el máximo de la barra al cargar el audio
            mediaPlayer?.setOnPreparedListener { mp ->
                seekBarAudio.max = mp.duration
            }

            // Al terminar
            mediaPlayer?.setOnCompletionListener {
                btnPlayPauseIcon.setImageResource(android.R.drawable.ic_media_play)
                seekBarAudio.progress = 0
                isPlaying = false
                if (::runnable.isInitialized) handler.removeCallbacks(runnable)
            }
        } catch (e: Exception) { }
    }

    private fun setupAudioControls() {
        // Clic en el icono Play/Pause
        btnPlayPauseIcon.setOnClickListener {
            if (isPlaying) pauseAudio() else playAudio()
        }

        // Mover la barra con el dedo
        seekBarAudio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun playAudio() {
        mediaPlayer?.start()
        isPlaying = true
        btnPlayPauseIcon.setImageResource(android.R.drawable.ic_media_pause)
        updateSeekBar()
    }

    private fun pauseAudio() {
        mediaPlayer?.pause()
        isPlaying = false
        btnPlayPauseIcon.setImageResource(android.R.drawable.ic_media_play)
        if (::runnable.isInitialized) handler.removeCallbacks(runnable)
    }

    private fun updateSeekBar() {
        runnable = Runnable {
            seekBarAudio.progress = mediaPlayer?.currentPosition ?: 0
            handler.postDelayed(runnable, 500)
        }
        handler.postDelayed(runnable, 0)
    }
    // --------------------------------------------

    private fun animateMascotaInicial() {
        ivMascotaPantalla1.startAnimation(AnimationUtils.loadAnimation(this, R.anim.mascot_bounce_in))
    }

    private fun animateMascotaSaludando() {
        ivMascotaPantalla2.startAnimation(AnimationUtils.loadAnimation(this, R.anim.mascot_wave))
    }

    private fun animateMascotaCelebracion() {
        ivMascotaPantalla2.setImageResource(R.drawable.mascota_celebrando)
        ivMascotaPantalla2.startAnimation(AnimationUtils.loadAnimation(this, R.anim.mascot_celebrate))
    }

    private fun mostrarPantallaTexto() {
        scrollTextContainer.visibility = View.VISIBLE
        sopaContainer.visibility = View.GONE
    }

    private fun mostrarSopaDeLetras() {
        // Pausar audio al cambiar de pantalla
        if (isPlaying) pauseAudio()

        scrollTextContainer.visibility = View.GONE
        sopaContainer.visibility = View.VISIBLE
        animateMascotaSaludando()
    }

    private fun setupWordSearchView() {
        wordSearchView.onWordFoundListener = { word, count ->
            foundWordsCount = count
            wordToCheckbox[word]?.isChecked = true

            // --- PUNTUACIÓN: +50 por palabra ---
            puntuacionActual += 50

            updateProgress()
            Toast.makeText(this, "✓ $word (+50 pts)", Toast.LENGTH_SHORT).show()

            if (foundWordsCount == totalWords) {
                onGameCompleted()
            }
        }
    }

    private fun setupFinishButton() {
        btnFinish.setOnClickListener {
            // Guardamos los puntos que lleve hasta ahora y salimos
            guardarPuntuacionEnBD(puntuacionActual)
            finish()
        }
    }

    private fun updateProgress() {
        tvProgress.text = "$foundWordsCount/$totalWords"
        btnFinish.isEnabled = foundWordsCount == totalWords
        if (foundWordsCount == totalWords) {
            btnFinish.backgroundTintList = ColorStateList.valueOf(Color.GREEN)
        }
    }

    private fun onGameCompleted() {
        // --- BONUS DE VICTORIA: +150 ---
        puntuacionActual += 150

        animateMascotaCelebracion()
        Toast.makeText(this, "🎉 Zorionak! (+150 Bonus)", Toast.LENGTH_LONG).show()

        // Guardamos la puntuación final (Total esperado: 500)
        guardarPuntuacionEnBD(puntuacionActual)
    }

    // --- FUNCIÓN PARA GUARDAR EN LA COLUMNA 'sopa' ---
    private fun guardarPuntuacionEnBD(puntos: Int) {
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombreAlumno = prefs.getString("nombre_alumno_actual", "Anonimo") ?: "Anonimo"

        val dbHelper = DatabaseHelper(this)

        // Enviamos "Sopa" como nombre de actividad para que DatabaseHelper sepa dónde guardar
        val guardado = dbHelper.guardarPuntuacion(nombreAlumno, "Sopa", puntos)

        if (guardado) {
            // Toast opcional para confirmar visualmente
            // Toast.makeText(this, "Gordeta: Sopa ($puntos)", Toast.LENGTH_SHORT).show()
        }
    }
}