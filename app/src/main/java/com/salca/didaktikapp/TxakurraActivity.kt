package com.salca.didaktikapp

import android.content.Context
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
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

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

    private lateinit var runnable: Runnable
    private var handler = Handler(Looper.getMainLooper())

    private lateinit var btnFinish: Button
    private lateinit var ivPerro: ImageView
    private lateinit var ivLeon: ImageView

    private val txakurraEditTexts = mutableListOf<EditText>()
    private val lehoiaEditTexts = mutableListOf<EditText>()
    private val allEditTexts = mutableListOf<EditText>()

    // ✅ LISTAS DE RESPUESTAS ACTUALIZADAS (Kanidoa / Felidoa)
    private val respuestasTxakurra = listOf("kanidoa", "etxea", "orojalea", "zaunka", "txikia")
    private val respuestasLehoia = listOf("felinoa", "sabana", "haragijalea", "orrua", "handia")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_txakurra)

        try {
            initializeViews()
            setupAudio()
            setupAudioControls()
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
        btnVolverMapa = findViewById(R.id.btnVolverMapa)

        btnFinish = findViewById(R.id.btnFinish)
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
                editText.setTextColor(Color.parseColor("#009900")) // Verde acierto
            } else {
                editText.setTextColor(Color.RED) // Rojo error
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
        }
        btnFinish.setOnClickListener {
            calcularPuntuacionFinal()
        }
    }

    // ✅ LÓGICA DE PUNTUACIÓN Y SUBIDA AL SERVIDOR
    private fun calcularPuntuacionFinal() {
        btnFinish.isEnabled = false

        var aciertos = 0
        txakurraEditTexts.forEachIndexed { index, et ->
            if (et.text.toString().trim().equals(respuestasTxakurra[index], ignoreCase = true)) aciertos++
        }
        lehoiaEditTexts.forEachIndexed { index, et ->
            if (et.text.toString().trim().equals(respuestasLehoia[index], ignoreCase = true)) aciertos++
        }

        // ✅ 50 Puntos por acierto (Total 500)
        val puntosObtenidos = aciertos * 50

        // ✅ GUARDAR EN BD LOCAL Y SUBIR A RENDER
        guardarPuntuacionEnBD(puntosObtenidos)
        SyncHelper.subirInmediatamente(this)

        // Salir a los 2 segundos
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 2000)
    }

    // Función auxiliar para SQLite
    private fun guardarPuntuacionEnBD(puntos: Int) {
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombreAlumno = prefs.getString("nombre_alumno_actual", "Anonimo") ?: "Anonimo"
        val dbHelper = DatabaseHelper(this)
        // Guardamos en la columna 'diferencias'
        dbHelper.guardarPuntuacion(nombreAlumno, "diferencias", puntos)
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

    override fun onDestroy() {
        super.onDestroy()
        if (::runnable.isInitialized) handler.removeCallbacks(runnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}