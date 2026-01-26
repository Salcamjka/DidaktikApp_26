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

// Ya no implementamos TextToSpeech.OnInitListener
class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap
    private var mapReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        SyncHelper.subirInmediatamente(this)

        if (mapReady) {
            actualizarTipoDeMapa()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        mapReady = true
        map.uiSettings.isZoomControlsEnabled = true
        map.setOnMarkerClickListener(this)

        actualizarTipoDeMapa()

        val kokapenak = listOf(
            Triple(LatLng(43.255000, -2.923333), "San Anton Eliza", "Urkatua"),
            Triple(LatLng(43.256389, -2.922222), "Antzinako Harresia", "Harresia"),
            Triple(LatLng(43.256389, -2.924722), "Zazpi Kaleak", "Letra Sopa"),
            Triple(LatLng(43.257833, -2.924389), "Txakurraren Iturria", "Ezberdintasunak"),
            Triple(LatLng(43.260221, -2.924074), "Bilboko Areatza", "Puzzlea")
        )

        for (puntu in kokapenak) {
            map.addMarker(MarkerOptions().position(puntu.first).title(puntu.second).snippet(puntu.third))
        }

        val centro = LatLng(43.2575, -2.9235)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(centro, 16.5f))
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
        // YA NO HAY VOZ AQUÍ, SOLO NAVEGACIÓN DIRECTA
        when (marker.title) {
            "Antzinako Harresia" -> startActivity(Intent(this, MurallaActivity::class.java))
            "Zazpi Kaleak" -> startActivity(Intent(this, SopaActivity::class.java))
            "Txakurraren Iturria" -> startActivity(Intent(this, TxakurraActivity::class.java))
            "Bilboko Areatza" -> startActivity(Intent(this, PuzzleActivity::class.java))
            "San Anton Eliza" -> startActivity(Intent(this, AhorcadoActivity::class.java))
        }
        return false
    }
}