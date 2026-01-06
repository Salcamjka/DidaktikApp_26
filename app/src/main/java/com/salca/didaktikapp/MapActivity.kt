package com.salca.didaktikapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap
    private val TAG = "MapActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        Log.d(TAG, "MapActivity onCreate")

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        Log.d(TAG, "Mapa listo")

        // Configuración del mapa
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isMapToolbarEnabled = false

        // IMPORTANTE: Asignar el listener ANTES de crear marcadores
        map.setOnMarkerClickListener(this)

        // 1. San Anton Eliza
        val sanAntonEliza = LatLng(43.256389, -2.924167)
        map.addMarker(
            MarkerOptions()
                .position(sanAntonEliza)
                .title("San Anton Eliza")
                .snippet("Actividad pendiente")
        )

        // 2. Antzinako Harresia
        val rondaKalea = LatLng(43.256389, -2.922778)
        map.addMarker(
            MarkerOptions()
                .position(rondaKalea)
                .title("Antzinako Harresia")
                .snippet("Actividad pendiente")
        )

        // 3. Zazpi Kaleak - SOPA DE LETRAS
        val zazpiKaleak = LatLng(43.256944, -2.924722)
        map.addMarker(
            MarkerOptions()
                .position(zazpiKaleak)
                .title("Zazpi Kaleak")
                .snippet("Toca para jugar sopa de letras")
        )

        // 4. Txakurraren Iturria
        val txakurrarenIturria = LatLng(43.257833, -2.924389)
        map.addMarker(
            MarkerOptions()
                .position(txakurrarenIturria)
                .title("Txakurraren Iturria")
                .snippet("Toca para jugar ")
        )

        // 5. Bilboko Areatza
        val bilbokoAreatza = LatLng(43.260278, -2.924167)
        map.addMarker(
            MarkerOptions()
                .position(bilbokoAreatza)
                .title("Bilboko Areatza")
                .snippet("Actividad pendiente")
        )

        // Centrar el mapa
        val cascoViejo = LatLng(43.257222, -2.924167)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(cascoViejo, 16.5f))

        Log.d(TAG, "Todos los marcadores creados. Listener configurado.")
    }

    // MÉTODO OBLIGATORIO de OnMarkerClickListener
    override fun onMarkerClick(marker: Marker): Boolean {
        Log.d(TAG, "========================================")
        Log.d(TAG, "¡MARCADOR TOCADO!")
        Log.d(TAG, "Título: ${marker.title}")
        Log.d(TAG, "========================================")

        // Mostrar Toast
        Toast.makeText(this, "Marcador: ${marker.title}", Toast.LENGTH_SHORT).show()

        // Verificar qué marcador es
        when (marker.title) {
            "Zazpi Kaleak" -> {
                Log.d(TAG, ">>> Abriendo SopaActivity <<<")

                // Pequeño delay para que se vea el Toast
                marker.showInfoWindow()

                // Abrir actividad
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this, SopaActivity::class.java)
                    startActivity(intent)
                }, 300)

                return true
            }

            "San Anton Eliza" -> {
                Toast.makeText(this, "San Anton Eliza - Próximamente", Toast.LENGTH_SHORT).show()
                marker.showInfoWindow()
                return true
            }

            "Antzinako Harresia" -> {
                Toast.makeText(this, "Antzinako Harresia - Próximamente", Toast.LENGTH_SHORT).show()
                marker.showInfoWindow()
                return true
            }

            "Txakurraren Iturria" -> {
                marker.showInfoWindow()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this, TxakurraActivity::class.java)
                    startActivity(intent)
                }, 300)
                return true
            }

            "Bilboko Areatza" -> {
                Toast.makeText(this, "Bilboko Areatza - Próximamente", Toast.LENGTH_SHORT).show()
                marker.showInfoWindow()
                return true
            }
        }

        // Por defecto, permitir el comportamiento normal
        return false
    }
}