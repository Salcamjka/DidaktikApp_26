package com.salca.didaktikapp

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.*
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
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

/**
 * Actividad principal que muestra el Mapa de Bilbao con los puntos de interés.
 *
 * Esta clase gestiona la integración con Google Maps y actúa como menú principal de la aplicación.
 * Sus funciones incluyen:
 * * Mostrar la ubicación de los 5 juegos didácticos.
 * * Gestionar el estado de los marcadores:
 * * **Rojo:** Actividad pendiente.
 * * **Gris:** Actividad completada (bloqueada).
 * * Permitir la navegación a las actividades al pulsar en los marcadores.
 * * Acceso a la pantalla de configuración (Accesibilidad).
 *
 * @author Salca
 * @version 1.0
 */
class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap
    private var mapReady = false
    private var modoTextoGrande = false

    /**
     * Objeto compañero para controlar el estado inicial de la aplicación.
     * Sirve para resetear configuraciones visuales solo la primera vez que se abre la app.
     */
    companion object {
        var esPrimeraVezEnLaApp = true
    }

    /**
     * Método de creación de la actividad.
     *
     * Inicializa el fragmento del mapa y configura el botón de ajustes.
     *
     * @param savedInstanceState Estado guardado de la instancia.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 1. Lógica de primer arranque ---
        // Si es la primera vez que se abre la app en esta sesión, desactivamos el texto grande por defecto
        if (esPrimeraVezEnLaApp) {
            val sharedPref = getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
            sharedPref.edit().putBoolean("MODO_TEXTO_GRANDE", false).apply()
            esPrimeraVezEnLaApp = false
        }

        // --- 2. Configuración de pantalla ---
        // Bloqueamos la rotación para evitar recargas innecesarias del mapa
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_map)

        // Botón para ir a los Ajustes (Accesibilidad)
        findViewById<ImageButton>(R.id.btnAjustes).setOnClickListener {
            startActivity(Intent(this, AccesibilidadActivity::class.java))
        }

        // --- 3. Carga asíncrona del mapa ---
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Método llamado cada vez que la actividad vuelve a primer plano.
     *
     * Es crucial para refrescar el estado de los marcadores (por si el usuario acaba de completar un juego)
     * y aplicar cambios de configuración visual.
     */
    override fun onResume() {
        super.onResume()
        // Intentamos subir datos pendientes al servidor
        SyncHelper.subirInmediatamente(this)

        leerPreferencias()

        // Si el mapa ya estaba cargado, actualizamos su aspecto y marcadores
        if (mapReady) {
            actualizarTipoDeMapa()
            recargarMarcadores()
        }
    }

    private fun leerPreferencias() {
        val sharedPref = getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
        modoTextoGrande = sharedPref.getBoolean("MODO_TEXTO_GRANDE", false)
    }

    /**
     * Callback que se ejecuta cuando el objeto GoogleMap está listo para usarse.
     *
     * @param googleMap Referencia al mapa cargado.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        mapReady = true

        // Habilitamos los controles de zoom (+/-)
        map.uiSettings.isZoomControlsEnabled = true

        // Asignamos el listener para detectar clics en los pinchos
        map.setOnMarkerClickListener(this)

        actualizarTipoDeMapa()
        crearMarcadores()

        // Centramos la cámara en el Casco Viejo de Bilbao con un zoom adecuado
        val centro = LatLng(43.2575, -2.9235)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(centro, 16.5f))
    }

    /**
     * Genera y coloca los marcadores en el mapa basándose en el progreso del alumno.
     */
    private fun crearMarcadores() {
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombreUsuario = prefs.getString("nombre_alumno_actual", "") ?: ""

        // Definición de las coordenadas y datos de cada punto de interés
        val puntos = listOf(
            Cuadruple(LatLng(43.255000, -2.923333), "San Anton Eliza", "Urkatua", "completado_ahorcado_$nombreUsuario"),
            Cuadruple(LatLng(43.256389, -2.922222), "Antzinako Harresia", "Harresia", "completado_muralla_$nombreUsuario"),
            Cuadruple(LatLng(43.256389, -2.924722), "Zazpi Kaleak", "Letra Sopa", "completado_sopa_$nombreUsuario"),
            Cuadruple(LatLng(43.257833, -2.924389), "Txakurraren Iturria", "Ezberdintasunak", "completado_txakurra_$nombreUsuario"),
            Cuadruple(LatLng(43.260221, -2.924074), "Bilboko Areatza", "Puzzlea", "completado_puzzle_$nombreUsuario")
        )

        for (punto in puntos) {
            // Comprobamos si este juego ya ha sido completado por el usuario actual
            val estaCompletado = prefs.getBoolean(punto.claveCompletado, false)

            // Elegimos el icono: Gris (personalizado) si está hecho, Rojo (default) si no
            val iconoMarcador = if (estaCompletado) {
                crearPinchoGris()
            } else {
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            }

            // Añadimos el marcador al mapa
            map.addMarker(
                MarkerOptions()
                    .position(punto.coordenadas)
                    .title(punto.titulo)
                    .snippet(punto.snippet)
                    .icon(iconoMarcador)
            )
        }
    }

    /** Limpia el mapa y vuelve a dibujar los marcadores (útil al volver de un juego). */
    private fun recargarMarcadores() {
        map.clear()
        crearMarcadores()
    }

    /**
     * Cambia el tipo de mapa entre Normal e Híbrido según la configuración de "Alto Contraste".
     */
    private fun actualizarTipoDeMapa() {
        val sharedPref = getSharedPreferences("AjustesApp", Context.MODE_PRIVATE)
        val modoOscuro = sharedPref.getBoolean("MODO_OSCURO", false)
        map.mapType = if (modoOscuro) GoogleMap.MAP_TYPE_HYBRID else GoogleMap.MAP_TYPE_NORMAL
    }

    // ================================================================
    // 🔒 LÓGICA DE NAVEGACIÓN Y BLOQUEO
    // ================================================================

    /**
     * Gestiona el clic en un marcador.
     *
     * Si la actividad ya está completada, muestra un mensaje y bloquea el acceso.
     * Si no, abre la actividad correspondiente.
     *
     * @param marker El marcador pulsado.
     * @return true para indicar que hemos consumido el evento.
     */
    override fun onMarkerClick(marker: Marker): Boolean {
        val prefs = getSharedPreferences("DidaktikAppPrefs", Context.MODE_PRIVATE)
        val nombreUsuario = prefs.getString("nombre_alumno_actual", "") ?: ""

        // Identificamos qué actividad es y cuál es su clave de completado según el título del marcador
        val (claveCompletado, claseActividad) = when (marker.title) {
            "Antzinako Harresia" -> Pair("completado_muralla_$nombreUsuario", MurallaActivity::class.java)
            "Zazpi Kaleak" -> Pair("completado_sopa_$nombreUsuario", SopaActivity::class.java)
            "Txakurraren Iturria" -> Pair("completado_txakurra_$nombreUsuario", TxakurraActivity::class.java)
            "Bilboko Areatza" -> Pair("completado_puzzle_$nombreUsuario", PuzzleActivity::class.java)
            "San Anton Eliza" -> Pair("completado_ahorcado_$nombreUsuario", AhorcadoActivity::class.java)
            else -> Pair(null, null)
        }

        if (claveCompletado != null && claseActividad != null) {
            // Comprobamos estado
            val yaHecho = prefs.getBoolean(claveCompletado, false)

            if (yaHecho) {
                // 🚫 BLOQUEADO: Feedback visual al usuario
                Toast.makeText(this, "Jarduera hau eginda dago! ✅", Toast.LENGTH_SHORT).show()
            } else {
                // ✅ LIBRE: Lanzamos la nueva actividad
                startActivity(Intent(this, claseActividad))
            }
        }

        return true
    }

    // ================================================================
    // DIBUJADO MANUAL DE MARCADORES (CANVAS)
    // ================================================================

    /**
     * Crea dinámicamente un icono de marcador de color GRIS.
     *
     * Google Maps no tiene un "HUE_GREY" nativo por defecto en versiones antiguas,
     * así que lo dibujamos manualmente usando Canvas para asegurar compatibilidad.
     *
     * @return Icono (BitmapDescriptor) listo para usar en el mapa.
     */
    private fun crearPinchoGris(): BitmapDescriptor {
        val width = 60
        val height = 90
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Color del cuerpo del pincho (Gris Oscuro)
        paint.color = Color.parseColor("#616161")
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true

        // Dibujar círculo superior
        val radio = 30f
        val centroX = 30f
        val centroY = 30f
        canvas.drawCircle(centroX, centroY, radio, paint)

        // Dibujar triángulo inferior (punta)
        val path = Path()
        path.moveTo(5f, 30f)
        path.lineTo(55f, 30f)
        path.lineTo(30f, 90f)
        path.close()
        canvas.drawPath(path, paint)

        // Dibujar punto negro central (decorativo)
        paint.color = Color.BLACK
        canvas.drawCircle(centroX, centroY, 8f, paint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    /**
     * Clase de datos auxiliar para almacenar la información de cada punto del mapa.
     */
    data class Cuadruple(
        val coordenadas: LatLng,
        val titulo: String,
        val snippet: String,
        val claveCompletado: String
    )
}