package com.salca.didaktikapp

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.collections.forEach

class MurallaActivity {

    class MainActivity : AppCompatActivity() {

        private lateinit var txtIntro: TextView
        private lateinit var txtTitulo: TextView
        private lateinit var btnComenzar: Button
        private lateinit var btnPlayPauseAudio: ImageButton
        private lateinit var seekBarAudio: SeekBar

        // Contenedor (ahora LinearLayout) y lista de piezas
        private lateinit var layoutMuralla: LinearLayout
        private lateinit var listaPiezas: List<ImageView>

        private lateinit var txtPregunta: TextView
        private lateinit var grupoOpciones: RadioGroup
        private lateinit var op1: RadioButton
        private lateinit var op2: RadioButton
        private lateinit var op3: RadioButton
        private lateinit var btnResponder: Button
        private lateinit var btnReintentar: Button

        private var audio: MediaPlayer? = null
        private val audioHandler = Handler(Looper.getMainLooper())
        private var isPlaying = false

        private var indicePregunta = 0
        private var progreso = 0

        // 5 Preguntas
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

            // Inicialización de vistas
            txtTitulo = findViewById(R.id.txtTitulo)
            txtIntro = findViewById(R.id.txtIntro)
            btnComenzar = findViewById(R.id.btnComenzar)
            btnPlayPauseAudio = findViewById(R.id.btnPlayPauseAudio)
            seekBarAudio = findViewById(R.id.seekBarAudio)

            layoutMuralla = findViewById(R.id.layoutMuralla)

            // Mapeamos las imágenes:
            // pieza0 (izquierda), pieza1 (centro-izq), etc.
            listaPiezas = listOf(
                findViewById(R.id.pieza0),
                findViewById(R.id.pieza1),
                findViewById(R.id.pieza2),
                findViewById(R.id.pieza3),
                findViewById(R.id.pieza4)
            )

            // AL PRINCIPIO: Todo invisible (hueco vacío total).
            // Al usar INVISIBLE en un LinearLayout con 'weight', el espacio se reserva.
            listaPiezas.forEach { it.visibility = View.INVISIBLE }

            txtPregunta = findViewById(R.id.txtPregunta)
            grupoOpciones = findViewById(R.id.grupoOpciones)
            op1 = findViewById(R.id.op1)
            op2 = findViewById(R.id.op2)
            op3 = findViewById(R.id.op3)
            btnResponder = findViewById(R.id.btnResponder)
            btnReintentar = findViewById(R.id.btnReintentar)

            btnComenzar.visibility = View.GONE

            txtIntro.text = """
            Orain dela urte asko, Bilbon harrizko harresi handi bat eraiki zen hiria babesteko asmoarekin.
            Bertan familia garrantzitsuenak bizi ziren, euren etxe, denda eta Katedralarekin. Harresitik
            kanpo, berriz, herri giroa zegoen, Pelota eta Ronda izeneko kaleetan.

            Denboraren poderioz, hiria hazi egin zen eta harresia ez zen hain beharrezkoa. Zati batzuk,
            gainean etxeak eraikitzeko erabili ziren, eta beste batzuk eraitsiz joan ziren, nahiz eta gaur
            egun ere egon zela gogorarazten diguten aztarnak dauden. Erronda kalean, esaterako,
            harresiaren gainean egindako fatxadak ikus daitezke, eta San Anton elizaren azpian ere
            aztarna garrantzitsuak daude.

            Harresiak bere oroitzapena utzi zuen Alde Zaharreko bi kaleren izenean. Pelota kaleari
            horrela deitzen zaio jendeak frontoi bat bezala erabiltzen zuelako harresia. Erronda kaleari,
            aldiz, harresia zaintzen zuten soldaduek guardiako txandak egiten zituztelako. Horregatik,
            gaur egun ere kale honek zaintza garai hura gogorarazten digu.
            """.trimIndent()

            mostrarTest(false)

            // Audio
            btnPlayPauseAudio.setOnClickListener {
                if (audio == null) {
                    audio = MediaPlayer.create(this, R.raw.audio_muralla)
                    audio?.setOnCompletionListener {
                        btnComenzar.visibility = View.VISIBLE
                        btnPlayPauseAudio.setImageResource(android.R.drawable.ic_media_play)
                        isPlaying = false
                        seekBarAudio.progress = 0
                    }
                    seekBarAudio.max = audio?.duration ?: 0
                    actualizarSeekBar()
                }
                if (isPlaying) {
                    audio?.pause()
                    btnPlayPauseAudio.setImageResource(android.R.drawable.ic_media_play)
                } else {
                    audio?.start()
                    btnPlayPauseAudio.setImageResource(android.R.drawable.ic_media_pause)
                }
                isPlaying = !isPlaying
            }

            btnComenzar.setOnClickListener {
                mostrarTest(true)
                mostrarPregunta()
            }

            btnResponder.setOnClickListener { comprobarRespuesta() }

            btnReintentar.setOnClickListener {
                indicePregunta = 0
                progreso = 0
                btnReintentar.visibility = View.GONE
                btnResponder.isEnabled = true

                // AL REINTENTAR: Ocultamos todas para empezar de cero
                listaPiezas.forEach { it.visibility = View.INVISIBLE }

                mostrarPregunta()
            }
        }

        private fun actualizarSeekBar() {
            val runnable = object : Runnable {
                override fun run() {
                    if (audio != null) seekBarAudio.progress = audio!!.currentPosition
                    audioHandler.postDelayed(this, 500)
                }
            }
            audioHandler.post(runnable)

            seekBarAudio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) audio?.seekTo(progress)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        private fun mostrarTest(visible: Boolean) {
            val vis = if (visible) View.VISIBLE else View.GONE

            layoutMuralla.visibility = vis
            txtPregunta.visibility = vis
            grupoOpciones.visibility = vis
            op1.visibility = vis
            op2.visibility = vis
            op3.visibility = vis
            btnResponder.visibility = vis

            txtIntro.visibility = if (visible) View.GONE else View.VISIBLE
            txtTitulo.visibility = if (visible) View.GONE else View.VISIBLE
            btnPlayPauseAudio.visibility = if (visible) View.GONE else View.VISIBLE
            seekBarAudio.visibility = if (visible) View.GONE else View.VISIBLE

            btnReintentar.visibility = View.GONE
            btnComenzar.visibility = if (visible) View.GONE else btnComenzar.visibility
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
                Toast.makeText(this, "Hautatu aukera bat", Toast.LENGTH_SHORT).show()
                return
            }

            val correcta = preguntas[indicePregunta].correcta

            if (seleccion == correcta) {
                progreso++

                // Si acierta, mostramos la pieza en SU columna correspondiente.
                // Las demás no se ven afectadas.
                if (indicePregunta < listaPiezas.size) {
                    listaPiezas[indicePregunta].visibility = View.VISIBLE
                }

                Toast.makeText(this, "Zuzena!", Toast.LENGTH_SHORT).show()
            } else {
                // Si falla, se queda invisible. Como es LinearLayout con weight,
                // el espacio se queda ahí, pero vacío (hueco).
                if (indicePregunta < listaPiezas.size) {
                    listaPiezas[indicePregunta].visibility = View.INVISIBLE
                }
                Toast.makeText(this, "Ez da zuzena", Toast.LENGTH_SHORT).show()
            }

            indicePregunta++

            if (indicePregunta < preguntas.size) {
                mostrarPregunta()
            } else {
                btnResponder.isEnabled = false
                if (progreso == preguntas.size) {
                    txtPregunta.text = "🏰 Zorionak! Harresia guztiz osatu da!"
                    btnReintentar.visibility = View.GONE
                } else {
                    txtPregunta.text = "Jokoa amaitu da. Puntuazioa: $progreso/${preguntas.size}"
                    btnReintentar.visibility = View.VISIBLE
                }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            audio?.release()
            audio = null
            audioHandler.removeCallbacksAndMessages(null)
        }
    }

    data class Pregunta(val enunciado: String, val opciones: List<String>, val correcta: Int)
}