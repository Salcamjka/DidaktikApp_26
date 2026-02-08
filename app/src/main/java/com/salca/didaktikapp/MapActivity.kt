package com.salca.didaktikapp

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.*
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast // ✅ Importante para el mensaje
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap
    private var mapReady = false
    private var modoTextoGrande = false

    companion object {
        var esPrimeraVezEnLaApp = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (esPrimeraVezEnLaApp) {
            val sharedPref = getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
            sharedPref.edit().putBoolean("MODO_TEXTO_GRANDE", false).apply()
            esPrimeraVezEnLaApp = false
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_map)

        findViewById<ImageButton>(R.id.btnAjustes).setOnClickListener {
            startActivity(Intent(this, AccesibilidadActivity::class.java))
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onResume() {
        super.onResume()
        SyncHelper.subirInmediatamente(this)
        leerPreferencias()
        if (mapReady) {
            actualizarTipoDeMapa()
            recargarMarcadores()
        }
    }

    private fun leerPreferencias() {
        val sharedPref = getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
        modoTextoGrande = sharedPref.getBoolean("MODO_TEXTO_GRANDE", false)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        mapReady = true
        map.uiSettings.isZoomControlsEnabled = true
        map.setOnMarkerClickListener(this)

        actualizarTipoDeMapa()
        crearMarcadores()

        val centro = LatLng(43.2575, -2.9235)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(centro, 16.5f))
    }

    private fun crearMarcadores() {
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombreUsuario = prefs.getString("nombre_alumno_actual", "") ?: ""

        val puntos = listOf(
            Cuadruple(LatLng(43.255000, -2.923333), "San Anton Eliza", "Urkatua", "completado_ahorcado_$nombreUsuario"),
            Cuadruple(LatLng(43.256389, -2.922222), "Antzinako Harresia", "Harresia", "completado_muralla_$nombreUsuario"),
            Cuadruple(LatLng(43.256389, -2.924722), "Zazpi Kaleak", "Letra Sopa", "completado_sopa_$nombreUsuario"),
            Cuadruple(LatLng(43.257833, -2.924389), "Txakurraren Iturria", "Ezberdintasunak", "completado_txakurra_$nombreUsuario"),
            Cuadruple(LatLng(43.260221, -2.924074), "Bilboko Areatza", "Puzzlea", "completado_puzzle_$nombreUsuario")
        )

        for (punto in puntos) {
            val estaCompletado = prefs.getBoolean(punto.claveCompletado, false)

            val iconoMarcador = if (estaCompletado) {
                // 🖌️ PINCHO GRIS
                crearPinchoGris()
            } else {
                // 🔴 PINCHO ROJO
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            }

            map.addMarker(
                MarkerOptions()
                    .position(punto.coordenadas)
                    .title(punto.titulo)
                    .snippet(punto.snippet)
                    .icon(iconoMarcador)
            )
        }
    }

    private fun recargarMarcadores() {
        map.clear()
        crearMarcadores()
    }

    private fun actualizarTipoDeMapa() {
        val sharedPref = getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
        val modoOscuro = sharedPref.getBoolean("MODO_OSCURO", false)
        map.mapType = if (modoOscuro) GoogleMap.MAP_TYPE_HYBRID else GoogleMap.MAP_TYPE_NORMAL
    }

    // ================================================================
    // 🔒 LÓGICA DE BLOQUEO DE ACTIVIDADES COMPLETADAS
    // ================================================================
    override fun onMarkerClick(marker: Marker): Boolean {
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombreUsuario = prefs.getString("nombre_alumno_actual", "") ?: ""

        // Identificamos qué actividad es y cuál es su clave de completado
        val (claveCompletado, claseActividad) = when (marker.title) {
            "Antzinako Harresia" -> Pair("completado_muralla_$nombreUsuario", MurallaActivity::class.java)
            "Zazpi Kaleak" -> Pair("completado_sopa_$nombreUsuario", SopaActivity::class.java)
            "Txakurraren Iturria" -> Pair("completado_txakurra_$nombreUsuario", TxakurraActivity::class.java)
            "Bilboko Areatza" -> Pair("completado_puzzle_$nombreUsuario", PuzzleActivity::class.java)
            "San Anton Eliza" -> Pair("completado_ahorcado_$nombreUsuario", AhorcadoActivity::class.java)
            else -> Pair(null, null)
        }

        if (claveCompletado != null && claseActividad != null) {
            // Comprobamos si ya está hecha
            val yaHecho = prefs.getBoolean(claveCompletado, false)

            if (yaHecho) {
                // 🚫 BLOQUEADO: Mostramos mensaje y NO abrimos la actividad
                Toast.makeText(this, "Jarduera hau eginda dago! ✅", Toast.LENGTH_SHORT).show()
            } else {
                // ✅ LIBRE: Abrimos la actividad
                startActivity(Intent(this, claseActividad))
            }
        }

        return true // Devolvemos true para indicar que hemos gestionado el click
    }

    // ================================================================
    // DIBUJAR PINCHO GRIS EN MEMORIA
    // ================================================================
    private fun crearPinchoGris(): BitmapDescriptor {
        val width = 60
        val height = 90
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        paint.color = Color.parseColor("#616161") // Gris Oscuro
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true

        val radio = 30f
        val centroX = 30f
        val centroY = 30f
        canvas.drawCircle(centroX, centroY, radio, paint)

        val path = Path()
        path.moveTo(5f, 30f)
        path.lineTo(55f, 30f)
        path.lineTo(30f, 90f)
        path.close()
        canvas.drawPath(path, paint)

        paint.color = Color.BLACK
        canvas.drawCircle(centroX, centroY, 8f, paint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    data class Cuadruple(
        val coordenadas: LatLng,
        val titulo: String,
        val snippet: String,
        val claveCompletado: String
    )
}