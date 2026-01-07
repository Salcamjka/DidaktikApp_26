package com.dm2.ahorcadodidaktikapp

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity

class AhorcadoAudioaActivity : AppCompatActivity() {

    // Variables globales
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var seekBar: SeekBar
    private lateinit var btnPlayPause: ImageButton

    // Handler para actualizar la barra de progreso cada segundo
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ahorcado_audioa)

        // 1. VINCULAR VISTAS CON EL XML
        btnPlayPause = findViewById(R.id.btnPlayPause)
        seekBar = findViewById(R.id.seekBarAudio)
        val btnJarraitu = findViewById<Button>(R.id.btnJarraitu)

        // 2. CARGAR EL AUDIO
        // Asegúrate de tener el archivo en res/raw/audioahorcado.mp3
        mediaPlayer = MediaPlayer.create(this, R.raw.audioahorcado)

        // Configurar el máximo del SeekBar a la duración total del audio
        mediaPlayer?.let {
            seekBar.max = it.duration
        }

        // 3. CONFIGURAR EL BOTÓN PLAY/PAUSE
        btnPlayPause.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                pausarAudio()
            } else {
                reproducirAudio()
            }
        }

        // 4. CONTROLAR LA BARRA (SEEKBAR) MANUALMENTE
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Si el usuario mueve la barra, actualizamos el audio a ese punto
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Opcional: Pausar mientras se arrastra si se desea
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Al soltar, nos aseguramos de que siga donde se dejó
            }
        })

        // 5. CUANDO EL AUDIO TERMINA SOLO
        mediaPlayer?.setOnCompletionListener {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
            seekBar.progress = 0 // Reiniciar barra
            handler.removeCallbacks(actualizarSeekBar) // Dejar de actualizar
        }

        // 6. BOTÓN JARRAITU (SALIR/CONTINUAR)
        btnJarraitu.setOnClickListener {
            finish()
        }
    }

    // --- FUNCIONES AUXILIARES ---

    private fun reproducirAudio() {
        mediaPlayer?.start()
        // Cambiamos el icono a PAUSA
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        // Empezamos a actualizar la barra
        handler.postDelayed(actualizarSeekBar, 0)
    }

    private fun pausarAudio() {
        mediaPlayer?.pause()
        // Cambiamos el icono a PLAY
        btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
        // Dejamos de actualizar la barra para ahorrar recursos
        handler.removeCallbacks(actualizarSeekBar)
    }

    // Runnable: Es una tarea que se repite para mover la barra sola
    private val actualizarSeekBar = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                // Actualizamos la posición actual de la barra
                seekBar.progress = player.currentPosition
                // Volvemos a ejecutar esto en 1000 milisegundos (1 segundo)
                handler.postDelayed(this, 500) // 500ms para que vaya más fluido
            }
        }
    }

    // --- LIMPIEZA DE MEMORIA ---
    override fun onDestroy() {
        super.onDestroy()
        // Detener la actualización de la barra
        handler.removeCallbacks(actualizarSeekBar)

        // Liberar el reproductor
        if (mediaPlayer != null) {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
}