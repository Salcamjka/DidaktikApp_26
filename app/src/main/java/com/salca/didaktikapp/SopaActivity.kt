package com.salca.didaktikapp

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity

class SopaActivity : AppCompatActivity() {

    private lateinit var scrollTextContainer: ScrollView
    private lateinit var tvTextoIntroductorio: TextView
    private lateinit var btnComenzarSopa: Button
    private lateinit var ivMascotaPantalla1: ImageView

    // Botón de audio único (solo pantalla 1)
    private lateinit var btnPlayPause: Button

    private lateinit var wordSearchView: WordSearchView
    private lateinit var tvProgress: TextView
    private lateinit var btnFinish: Button
    private lateinit var sopaContainer: ScrollView
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

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sopa)

        initializeViews()
        setupAudioControls()
        setupWordSearchView()
        setupFinishButton()

        mostrarPantallaTexto()
        animateMascotaInicial()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun initializeViews() {
        // Pantalla 1
        scrollTextContainer = findViewById(R.id.scrollTextContainer)
        tvTextoIntroductorio = findViewById(R.id.tvTextoIntroductorio)
        btnComenzarSopa = findViewById(R.id.btnComenzarSopa)
        ivMascotaPantalla1 = findViewById(R.id.ivMascotaPantalla1)

        // Botón de audio (solo pantalla 1)
        btnPlayPause = findViewById(R.id.btnPlayPause)

        // Pantalla 2
        sopaContainer = findViewById(R.id.sopaContainer)
        wordSearchView = findViewById(R.id.wordSearchView)
        tvProgress = findViewById(R.id.tvProgress)
        btnFinish = findViewById(R.id.btnFinish)
        ivMascotaPantalla2 = findViewById(R.id.ivMascotaPantalla2)

        // CheckBoxes
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

        btnComenzarSopa.setOnClickListener {
            mostrarSopaDeLetras()
        }
    }

    // ========== CONTROL DE AUDIO SIMPLIFICADO (SOLO PANTALLA 1) ==========

    private fun setupAudioControls() {
        // Inicializar MediaPlayer
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.jarduera_3)
            mediaPlayer?.isLooping = true
        } catch (e: Exception) {
            Toast.makeText(this, "Audio no disponible", Toast.LENGTH_SHORT).show()
        }

        // Botón Play/Pause - Solo Pantalla 1
        btnPlayPause.setOnClickListener {
            if (isPlaying) {
                pauseAudio()
            } else {
                playAudio()
            }
        }
    }

    private fun playAudio() {
        try {
            mediaPlayer?.start()
            isPlaying = true
            updateAudioButton()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al reproducir audio", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pauseAudio() {
        mediaPlayer?.pause()
        isPlaying = false
        updateAudioButton()
    }

    private fun updateAudioButton() {
        if (isPlaying) {
            btnPlayPause.text = "⏸️ Pausatu"
        } else {
            btnPlayPause.text = "🔊 Entzun audioa"
        }

        // Mantener color negro siempre
        btnPlayPause.backgroundTintList = getColorStateList(android.R.color.black)
    }

    private fun animateMascotaInicial() {
        val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_bounce_in)
        ivMascotaPantalla1.startAnimation(bounceAnim)
    }

    private fun animateMascotaSaludando() {
        val waveAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_wave)
        ivMascotaPantalla2.startAnimation(waveAnim)
    }

    private fun animateMascotaPalabraEncontrada() {
        val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_bounce_in)
        ivMascotaPantalla2.startAnimation(bounceAnim)
    }

    private fun animateMascotaCelebracion() {
        ivMascotaPantalla2.setImageResource(R.drawable.mascota_celebrando)
        val celebrateAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_celebrate)
        ivMascotaPantalla2.startAnimation(celebrateAnim)
    }

    private fun mostrarPantallaTexto() {
        scrollTextContainer.visibility = View.VISIBLE
        sopaContainer.visibility = View.GONE
    }

    private fun mostrarSopaDeLetras() {
        scrollTextContainer.visibility = View.GONE
        sopaContainer.visibility = View.VISIBLE
        animateMascotaSaludando()
    }

    private fun setupWordSearchView() {
        wordSearchView.onWordFoundListener = { word, count ->
            foundWordsCount = count
            wordToCheckbox[word]?.isChecked = true
            updateProgress()
            showWordFoundMessage(word)
            animateMascotaPalabraEncontrada()

            if (foundWordsCount == totalWords) {
                onGameCompleted()
            }
        }
    }

    private fun setupFinishButton() {
        btnFinish.setOnClickListener {
            finish()
        }
    }

    private fun updateProgress() {
        tvProgress.text = "$foundWordsCount/$totalWords"
        btnFinish.isEnabled = foundWordsCount == totalWords

        if (foundWordsCount == totalWords) {
            btnFinish.backgroundTintList = getColorStateList(android.R.color.holo_green_light)
        }
    }

    private fun showWordFoundMessage(word: String) {
        val displayName = when (word) {
            "SOMERA" -> "Somera"
            "ARTEKALE" -> "Artekale"
            "TENDERIA" -> "Tendería"
            "BELOSTIKALE" -> "Belostikale"
            "CARNICERIAVIEJA" -> "Carnicería Vieja"
            "BARRENKALE" -> "Barrenkale"
            "BARRENKALEBARRENA" -> "Barrenkale Barrena"
            else -> word
        }

        Toast.makeText(this, "✓ $displayName aurkituta!", Toast.LENGTH_SHORT).show()
    }

    private fun onGameCompleted() {
        animateMascotaCelebracion()
        Toast.makeText(this, "🎉 Zorionak! Hitz guztiak aurkitu dituzu!", Toast.LENGTH_LONG).show()
    }
}