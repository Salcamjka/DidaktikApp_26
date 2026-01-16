package com.salca.didaktikapp

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TxakurraActivity : AppCompatActivity() {

    // Componentes de Audio
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private lateinit var btnPlayPauseIcon: ImageButton
    private lateinit var seekBarAudio: SeekBar

    // Para actualizar la barra
    private lateinit var runnable: Runnable
    private var handler = Handler(Looper.getMainLooper())

    // Otros componentes
    private lateinit var btnContinuar: Button
    private lateinit var ivMascota: ImageView

    // NUEVO: Botón Mapa
    private lateinit var btnVolverMapa: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_txakurra)

        initializeViews()
        setupAudioControls()
        setupContinuarButton()
        setupAudio()
        animateMascota()
    }

    private fun initializeViews() {
        btnPlayPauseIcon = findViewById(R.id.btnPlayPauseIcon)
        seekBarAudio = findViewById(R.id.seekBarAudio)
        btnContinuar = findViewById(R.id.btnContinuar)
        ivMascota = findViewById(R.id.ivMascota)

        // --- CONFIGURACIÓN BOTÓN VOLVER AL MAPA ---
        btnVolverMapa = findViewById(R.id.btnVolverMapa)
        btnVolverMapa.setOnClickListener {
            // Parar audio si está sonando
            if (::runnable.isInitialized) handler.removeCallbacks(runnable)
            mediaPlayer?.stop()
            // Volver al mapa
            finish()
        }
    }

    private fun animateMascota() {
        val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.mascot_bounce_in)
        ivMascota.startAnimation(bounceAnim)
    }

    private fun setupAudio() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.jarduera_4)

            mediaPlayer?.setOnPreparedListener { mp ->
                seekBarAudio.max = mp.duration
            }

            mediaPlayer?.setOnCompletionListener {
                btnPlayPauseIcon.setImageResource(android.R.drawable.ic_media_play)
                seekBarAudio.progress = 0
                isPlaying = false
                if (::runnable.isInitialized) {
                    handler.removeCallbacks(runnable)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Errorea audioarekin", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAudioControls() {
        btnPlayPauseIcon.setOnClickListener {
            if (isPlaying) pauseAudio() else playAudio()
        }

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
        if (::runnable.isInitialized) {
            handler.removeCallbacks(runnable)
        }
    }

    private fun updateSeekBar() {
        runnable = Runnable {
            seekBarAudio.progress = mediaPlayer?.currentPosition ?: 0
            handler.postDelayed(runnable, 500)
        }
        handler.postDelayed(runnable, 0)
    }

    private fun setupContinuarButton() {
        btnContinuar.setOnClickListener {
            // Detener y liberar audio al cambiar de pantalla
            if (::runnable.isInitialized) {
                handler.removeCallbacks(runnable)
            }

            try {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.stop()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            mediaPlayer?.release()
            mediaPlayer = null

            // Cambiar a la actividad de la Tabla (allí no habrá botón de mapa)
            val intent = Intent(this, TxakurraTablaActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::runnable.isInitialized) handler.removeCallbacks(runnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}