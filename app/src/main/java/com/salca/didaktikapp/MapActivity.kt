package com.salca.didaktikapp

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory // ✅ NUEVO: Para cambiar colores

/**
 * Actividad principal del mapa
 * Muestra los marcadores de las actividades y cambia su color según el progreso del usuario
 */
class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap
    private var mapReady = false

    // VARIABLES PREFERENCIAS
    private var modoTextoGrande = false

    // --- VARIABLES DE SESIÓN (ESTÁTICAS) ---
    companion object {
        var esPrimeraVezEnLaApp = true
    }
    // ----------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si es la primera vez que se carga la app, reseteamos el tamaño de letra
        if (esPrimeraVezEnLaApp) {
            val sharedPref = getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putBoolean("MODO_TEXTO_GRANDE", false)
            editor.apply()

            esPrimeraVezEnLaApp = false
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_map)

        val btnAjustes = findViewById<ImageButton>(R.id.btnAjustes)
        btnAjustes.setOnClickListener {
            startActivity(Intent(this, AccesibilidadActivity::class.java))
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onResume() {
        super.onResume()
        // Subimos datos si hay pendientes
        SyncHelper.subirInmediatamente(this)

        // Leer configuración visual
        leerPreferencias()

        // ========================================
        // ✅ NUEVO: Actualizar marcadores cuando volvemos al mapa
        // Esto cambia los colores según qué actividades completó el usuario actual
        // ========================================
        if (mapReady) {
            actualizarTipoDeMapa()
            recargarMarcadores() // ✅ NUEVO
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

        // ========================================
        // ✅ MODIFICADO: Ahora usamos función separada para crear marcadores
        // ========================================
        crearMarcadores()

        val centro = LatLng(43.2575, -2.9235)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(centro, 16.5f))
    }

    /**
     * ✅ NUEVA FUNCIÓN: Crea los marcadores con el color correcto
     * según si la actividad está completada POR EL USUARIO ACTUAL
     */
    private fun crearMarcadores() {
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        // ✅ Obtener el nombre del usuario actual
        val nombreUsuario = prefs.getString("nombre_alumno_actual", "") ?: ""

        // ========================================
        // Definición de todos los puntos del mapa
        // Cada punto tiene: Coordenadas, Título, Snippet, Clave de completado
        // ✅ IMPORTANTE: La clave incluye el nombre del usuario
        // ========================================
        val puntos = listOf(
            Cuadruple(LatLng(43.255000, -2.923333), "San Anton Eliza", "Urkatua", "completado_ahorcado_$nombreUsuario"),
            Cuadruple(LatLng(43.256389, -2.922222), "Antzinako Harresia", "Harresia", "completado_muralla_$nombreUsuario"),
            Cuadruple(LatLng(43.256389, -2.924722), "Zazpi Kaleak", "Letra Sopa", "completado_sopa_$nombreUsuario"),
            Cuadruple(LatLng(43.257833, -2.924389), "Txakurraren Iturria", "Ezberdintasunak", "completado_txakurra_$nombreUsuario"),
            Cuadruple(LatLng(43.260221, -2.924074), "Bilboko Areatza", "Puzzlea", "completado_puzzle_$nombreUsuario")
        )

        // Crear cada marcador con su color correspondiente
        for (punto in puntos) {
            // Verifica si ESTE USUARIO completó la actividad
            val estaCompletado = prefs.getBoolean(punto.claveCompletado, false)

            // ========================================
            // LÓGICA DE COLOR:
            // - GRIS/AZUL (HUE_AZURE) si el usuario la completó
            // - ROJO (HUE_RED) si el usuario NO la ha completado
            // ========================================
            val color = if (estaCompletado) {
                BitmapDescriptorFactory.HUE_CYAN// Gris/Azul grisáceo
            } else {
                BitmapDescriptorFactory.HUE_RED   // Rojo (por defecto)
            }

            // Crear el marcador con el color correspondiente
            map.addMarker(
                MarkerOptions()
                    .position(punto.coordenadas)
                    .title(punto.titulo)
                    .snippet(punto.snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(color))
            )
        }
    }

    /**
     * ✅ NUEVA FUNCIÓN: Recarga todos los marcadores
     * Se llama cuando volvemos de una actividad al mapa (en onResume)
     */
    private fun recargarMarcadores() {
        map.clear() // Borra todos los marcadores actuales del mapa
        crearMarcadores() // Vuelve a crearlos con los colores actualizados
    }

    private fun actualizarTipoDeMapa() {
        val sharedPref = getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
        val modoOscuro = sharedPref.getBoolean("MODO_OSCURO", false)

        if (modoOscuro) {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
        } else {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        when (marker.title) {
            "Antzinako Harresia" -> startActivity(Intent(this, MurallaActivity::class.java))
            "Zazpi Kaleak" -> startActivity(Intent(this, SopaActivity::class.java))
            "Txakurraren Iturria" -> startActivity(Intent(this, TxakurraActivity::class.java))
            "Bilboko Areatza" -> startActivity(Intent(this, PuzzleActivity::class.java))
            "San Anton Eliza" -> startActivity(Intent(this, AhorcadoActivity::class.java))
        }
        return false
    }

    /**
     * ✅ NUEVA CLASE AUXILIAR: Para agrupar datos de cada punto del mapa
     * Es como una tupla de 4 elementos
     */
    data class Cuadruple(
        val coordenadas: LatLng,
        val titulo: String,
        val snippet: String,
        val claveCompletado: String
    )
}