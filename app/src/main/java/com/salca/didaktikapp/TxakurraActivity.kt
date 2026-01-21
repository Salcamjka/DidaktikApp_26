package com.salca.didaktikapp

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class TxakurraActivity : AppCompatActivity() {

    // Contenedores Principales
    private lateinit var mainScrollView: ScrollView
    private lateinit var contenedorIntro: LinearLayout
    private lateinit var contenedorTabla: LinearLayout

    // Componentes Intro (Parte 1)
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private lateinit var btnPlayPauseIcon: ImageButton
    private lateinit var seekBarAudio: SeekBar
    private lateinit var btnVolverMapa: ImageButton
    private lateinit var btnContinuar: Button
    private lateinit var ivMascotaIntro: ImageView

    // Audio Handler
    private lateinit var runnable: Runnable
    private var handler = Handler(Looper.getMainLooper())

    // Componentes Tabla (Parte 2)
    private lateinit var btnFinish: Button
    private lateinit var ivMascotaTabla: ImageView
    private lateinit var ivPerro: ImageView
    private lateinit var ivLeon: ImageView

    private val allEditTexts = mutableListOf<EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_txakurra)

        initializeViews()
        setupAudioControls()
        setupAudio()
        animateMascotaIntro()
        setupTextWatchers()
        setupNavigationButtons()
    }

    private fun initializeViews() {
        mainScrollView = findViewById(R.id.mainScrollView)
        contenedorIntro = findViewById(R.id.contenedorIntro)
        contenedorTabla = findViewById(R.id.contenedorTabla)

        // Intro
        btnPlayPauseIcon = findViewById(R.id.btnPlayPauseIcon)
        seekBarAudio = findViewById(R.id.seekBarAudio)
        btnContinuar = findViewById(R.id.btnContinuar)
        ivMascotaIntro = findViewById(R.id.ivMascotaIntro)
        btnVolverMapa = findViewById(R.id.btnVolverMapa)

        // Tabla
        btnFinish = findViewById(R.id.btnFinish)
        ivMascotaTabla = findViewById(R.id.ivMascotaTabla)
        ivPerro = findViewById(R.id.ivPerro)
        ivLeon = findViewById(R.id.ivLeon)

        // EditTexts
        val ids = listOf(
            R.id.etLehoia1, R.id.etLehoia2, R.id.etLehoia3, R.id.etLehoia4, R.id.etLehoia5,
            R.id.etTxakurra1, R.id.etTxakurra2, R.id.etTxakurra3, R.id.etTxakurra4, R.id.etTxakurra5
        )
        ids.forEach { id -> allEditTexts.add(findViewById(id)) }
    }

    // ==========================================
    // AUDIO
    // ==========================================
    private fun setupAudio() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.jarduera_4)
            mediaPlayer?.setOnPreparedListener { mp -> seekBarAudio.max = mp.duration }
            mediaPlayer?.setOnCompletionListener {
                btnPlayPauseIcon.setImageResource(android.R.drawable.ic_media_play)
                seekBarAudio.progress = 0
                isPlaying = false
                if (::runnable.isInitialized) handler.removeCallbacks(runnable)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Errorea audioarekin", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAudioControls() {
        btnPlayPauseIcon.setOnClickListener { if (isPlaying) pauseAudio() else playAudio() }

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

    private fun animateMascotaIntro() {
        val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_bounce_in)
        ivMascotaIntro.startAnimation(bounceAnim)
    }

    // ==========================================
    // NAVEGACIÓN Y TABLA
    // ==========================================

    private fun setupNavigationButtons() {
        btnVolverMapa.visibility = View.VISIBLE

        btnVolverMapa.setOnClickListener {
            if (isPlaying) pauseAudio()
            finish()
        }

        btnContinuar.setOnClickListener {
            if (::runnable.isInitialized) handler.removeCallbacks(runnable)
            mediaPlayer?.stop()
            isPlaying = false
            btnPlayPauseIcon.setImageResource(android.R.drawable.ic_media_play)

            contenedorIntro.visibility = View.GONE
            contenedorTabla.visibility = View.VISIBLE
            btnVolverMapa.visibility = View.GONE
            mainScrollView.scrollTo(0, 0)

            // LLAMADA A LA FUNCIÓN SIN ANIMACIONES PARA LOS ANIMALES
            animateTablaElements()
        }

        btnFinish.setOnClickListener {
            animateMascotaCelebracion()
            guardarPuntuacionEnBD(500)
            SyncHelper.subirInmediatamente(this)
            Toast.makeText(this, "🎉 Bikain! Taula osatu duzu! (+500 pts)", Toast.LENGTH_LONG).show()
            Handler(Looper.getMainLooper()).postDelayed({ finish() }, 2000)
        }
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { validarTabla() }
        }
        allEditTexts.forEach { it.addTextChangedListener(watcher) }
    }

    private fun validarTabla() {
        val estaCompleto = allEditTexts.all { it.text.toString().trim().isNotEmpty() }
        btnFinish.isEnabled = estaCompleto
        if (estaCompleto) {
            val colorActivo = ContextCompat.getColor(this, R.color.txakurra)
            btnFinish.backgroundTintList = ColorStateList.valueOf(colorActivo)
        } else {
            val colorDesactivado = ContextCompat.getColor(this, R.color.boton_desactivado)
            btnFinish.backgroundTintList = ColorStateList.valueOf(colorDesactivado)
        }
    }

    private fun animateTablaElements() {
        // HEMOS QUITADO LAS ANIMACIONES DE IVPERRO E IVLEON
        try {
            val waveAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_wave)
            ivMascotaTabla.startAnimation(waveAnim)
        } catch (e: Exception) { }
    }

    private fun animateMascotaCelebracion() {
        try {
            ivMascotaTabla.setImageResource(R.drawable.leonfeliz)
            val celebrateAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_celebrate)
            ivMascotaTabla.startAnimation(celebrateAnim)
        } catch (e: Exception) { }
    }

    private fun guardarPuntuacionEnBD(puntos: Int) {
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombreAlumno = prefs.getString("nombre_alumno_actual", "Anonimo") ?: "Anonimo"
        val dbHelper = DatabaseHelper(this)
        dbHelper.guardarPuntuacion(nombreAlumno, "Txakurra", puntos)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::runnable.isInitialized) handler.removeCallbacks(runnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}