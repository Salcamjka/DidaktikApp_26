package com.salca.didaktikapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // ================================================================
    //  ⚡ ON STOP: CUANDO CIERRAS LA APP
    // ================================================================
    override fun onStop() {
        super.onStop()
        programarSubidaSegura()
    }

    private fun programarSubidaSegura() {
        // Creamos la petición de trabajo para el "UploadWorker"
        val uploadWork = OneTimeWorkRequestBuilder<UploadWorker>().build()

        // Le decimos al sistema: "Ejecuta esto. Si ya hay uno pendiente, reemplázalo por este nuevo"
        WorkManager.getInstance(this).enqueueUniqueWork(
            "SubidaDatosDB",
            ExistingWorkPolicy.REPLACE,
            uploadWork
        )
    }

    // --- (El resto del código del mapa sigue IGUAL) ---
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.setOnMarkerClickListener(this)

        val kokapenak = listOf(
            Triple(LatLng(43.255000, -2.923333), "San Anton Eliza", "Zain"),
            Triple(LatLng(43.256389, -2.922222), "Antzinako Harresia", "Toca para jugar"),
            Triple(LatLng(43.256389, -2.924722), "Zazpi Kaleak", "Sopa de letras"),
            Triple(LatLng(43.257833, -2.924389), "Txakurraren Iturria", "Jolastu"),
            Triple(LatLng(43.260221, -2.924074), "Bilboko Areatza", "Puzzlea")
        )

        for (puntu in kokapenak) {
            map.addMarker(MarkerOptions().position(puntu.first).title(puntu.second).snippet(puntu.third))
        }

        val centro = LatLng(43.2575, -2.9235)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(centro, 16.5f))
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        when (marker.title) {
            "Antzinako Harresia" -> { startActivity(Intent(this, MurallaActivity::class.java)); return true }
            "Zazpi Kaleak" -> { startActivity(Intent(this, SopaActivity::class.java)); return true }
            "Txakurraren Iturria" -> { startActivity(Intent(this, TxakurraActivity::class.java)); return true }
            "Bilboko Areatza" -> { startActivity(Intent(this, PuzzleActivity::class.java)); return true }
            "San Anton Eliza" -> { startActivity(Intent(this, AhorcadoActivity::class.java)); return true }
        }
        marker.showInfoWindow()
        return false
    }
}