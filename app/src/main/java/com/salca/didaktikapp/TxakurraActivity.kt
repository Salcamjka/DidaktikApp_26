package com.salca.didaktikapp

import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class TxakurraActivity : AppCompatActivity() {

    private lateinit var mainScrollView: ScrollView
    private lateinit var contenedorIntro: LinearLayout
    private lateinit var contenedorTabla: LinearLayout

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private lateinit var btnPlayPauseIcon: ImageButton
    private lateinit var seekBarAudio: SeekBar
    private lateinit var btnVolverMapa: ImageButton
    private lateinit var btnContinuar: Button
    private lateinit var ivMascotaIntro: ImageView

    private lateinit var runnable: Runnable
    private var handler = Handler(Looper.getMainLooper())

    private lateinit var btnFinish: Button
    private lateinit var ivMascotaTabla: ImageView
    private lateinit var ivPerro: ImageView
    private lateinit var ivLeon: ImageView

    private val txakurraEditTexts = mutableListOf<EditText>()
    private val lehoiaEditTexts = mutableListOf<EditText>()
    private val allEditTexts = mutableListOf<EditText>()

    private val respuestasTxakurra = listOf("etxekoa", "etxea", "orojalea", "zaunka", "txikia")
    private val respuestasLehoia = listOf("basatia", "sabana", "haragijalea", "orrua", "handia")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_txakurra)

        try {
            initializeViews()
            setupAudio()
            setupAudioControls()
            animateMascotaIntro()
            setupTextWatchers()
            setupIndividualValidation()
            setupNavigationButtons()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al iniciar: " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeViews() {
        mainScrollView = findViewById(R.id.mainScrollView)
        contenedorIntro = findViewById(R.id.contenedorIntro)
        contenedorTabla = findViewById(R.id.contenedorTabla)

        btnPlayPauseIcon = findViewById(R.id.btnPlayPauseIcon)
        seekBarAudio = findViewById(R.id.seekBarAudio)
        btnContinuar = findViewById(R.id.btnContinuar)
        ivMascotaIntro = findViewById(R.id.ivMascotaIntro)
        btnVolverMapa = findViewById(R.id.btnVolverMapa)

        btnFinish = findViewById(R.id.btnFinish)
        ivMascotaTabla = findViewById(R.id.ivMascotaTabla)
        ivPerro = findViewById(R.id.ivPerro)
        ivLeon = findViewById(R.id.ivLeon)

        val idsTxakurra = listOf(R.id.etTxakurra1, R.id.etTxakurra2, R.id.etTxakurra3, R.id.etTxakurra4, R.id.etTxakurra5)
        val idsLehoia = listOf(R.id.etLehoia1, R.id.etLehoia2, R.id.etLehoia3, R.id.etLehoia4, R.id.etLehoia5)

        idsTxakurra.forEach { id ->
            val et = findViewById<EditText>(id)
            txakurraEditTexts.add(et)
            allEditTexts.add(et)
        }
        idsLehoia.forEach { id ->
            val et = findViewById<EditText>(id)
            lehoiaEditTexts.add(et)
            allEditTexts.add(et)
        }
    }

    private fun setupIndividualValidation() {
        txakurraEditTexts.forEachIndexed { index, editText ->
            val respuestaCorrecta = respuestasTxakurra[index]
            editText.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                    validarUnSoloCampo(editText, respuestaCorrecta)
                }
                false
            }
            editText.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) validarUnSoloCampo(editText, respuestaCorrecta) }
        }

        lehoiaEditTexts.forEachIndexed { index, editText ->
            val respuestaCorrecta = respuestasLehoia[index]
            editText.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                    validarUnSoloCampo(editText, respuestaCorrecta)
                }
                false
            }
            editText.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) validarUnSoloCampo(editText, respuestaCorrecta) }
        }
    }

    private fun validarUnSoloCampo(editText: EditText, respuestaCorrecta: String) {
        val textoUsuario = editText.text.toString().trim()
        if (textoUsuario.isNotEmpty()) {
            if (textoUsuario.equals(respuestaCorrecta, ignoreCase = true)) {
                // VERDE ESMERALDA (#009900)
                editText.setTextColor(Color.parseColor("#009900"))
            } else {
                editText.setTextColor(Color.RED)
            }
        } else {
            editText.setTextColor(Color.BLACK)
        }
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { verificarCamposLlenos() }
        }
        allEditTexts.forEach { it.addTextChangedListener(watcher) }
    }

    private fun verificarCamposLlenos() {
        val estaCompleto = allEditTexts.all { it.text.toString().trim().isNotEmpty() }
        btnFinish.isEnabled = estaCompleto
        if (estaCompleto) {
            btnFinish.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E57373"))
        } else {
            btnFinish.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9E9E9E"))
        }
    }

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
            animateTablaElements()
        }
        btnFinish.setOnClickListener {
            calcularPuntuacionFinal()
        }
    }

    // --- AQUÍ ESTÁ EL CAMBIO PARA SALIR SIEMPRE ---
    private fun calcularPuntuacionFinal() {
        // Desactivamos el botón para que no le den dos veces
        btnFinish.isEnabled = false

        var aciertos = 0
        txakurraEditTexts.forEachIndexed { index, et ->
            if (et.text.toString().trim().equals(respuestasTxakurra[index], ignoreCase = true)) aciertos++
        }
        lehoiaEditTexts.forEachIndexed { index, et ->
            if (et.text.toString().trim().equals(respuestasLehoia[index], ignoreCase = true)) aciertos++
        }

        val puntosObtenidos = aciertos * 50

        // Mensaje diferente según la nota
        if (aciertos == 10) {
            animateMascotaCelebracion()
        }

        // --- IMPORTANTE: SALIR SIEMPRE DESPUÉS DE 2 SEGUNDOS ---
        // Da igual si tiene 0, 5 o 10 aciertos, se cierra la actividad.
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 2000)
    }

    // --- AUDIO ---
    private fun setupAudio() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.jarduera_4)
            if (mediaPlayer != null) {
                mediaPlayer?.setOnPreparedListener { mp -> seekBarAudio.max = mp.duration }
                mediaPlayer?.setOnCompletionListener {
                    btnPlayPauseIcon.setImageResource(android.R.drawable.ic_media_play)
                    seekBarAudio.progress = 0
                    isPlaying = false
                    if (::runnable.isInitialized) handler.removeCallbacks(runnable)
                }
            }
        } catch (e: Exception) {}
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

    // --- ANIMACIONES ---
    private fun animateMascotaIntro() {
        try {
            val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_bounce_in)
            ivMascotaIntro.startAnimation(bounceAnim)
        } catch (e: Exception) { }
    }

    private fun animateTablaElements() {
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

    override fun onDestroy() {
        super.onDestroy()
        if (::runnable.isInitialized) handler.removeCallbacks(runnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}