package com.salca.didaktikapp

import android.content.Context
import android.util.Log
import org.json.JSONArray
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Singleton encargado de la sincronización de datos con el servidor remoto (API).
 *
 * Sus funciones principales son:
 * * **Subida de datos (Backup):** Envía el archivo de base de datos local (.db) al servidor.
 * * **Bajada de datos (Ranking):** Descarga la lista de mejores puntuaciones globales.
 *
 * Utiliza conexiones HTTP nativas (`HttpURLConnection`) para no depender de librerías externas pesadas.
 *
 * @author Nizam
 * @version 1.0
 */
object SyncHelper {

    // Configuración de la API (alojada en Render)
    private const val BASE_URL = "https://api-didaktikapp.onrender.com"
    private const val UPLOAD_URL = "$BASE_URL/upload-db"
    private const val RANKING_URL = "$BASE_URL/ranking"

    /**
     * Sube la base de datos local al servidor en un hilo secundario.
     *
     * Antes de subir, fuerza un "Checkpoint" en la base de datos para asegurar
     * que todos los datos en memoria se escriban en el archivo físico.
     *
     * @param context Contexto de la aplicación necesario para localizar el archivo de la BD.
     */
    fun subirInmediatamente(context: Context) {
        Thread {
            // Localizamos el archivo físico de la base de datos
            val dbPath = context.getDatabasePath("DidaktikApp.db")
            if (!dbPath.exists()) return@Thread

            // --- TRUCO IMPORTANTE ---
            // SQLite a veces guarda datos en archivos temporales (-wal, -shm).
            // Forzamos un 'checkpoint' para que TODO se escriba en el archivo .db principal
            // antes de enviarlo.
            try {
                val dbHelper = DatabaseHelper(context)
                val db = dbHelper.writableDatabase
                db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).close()
                db.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Procedemos a la subida
            uploadFile(dbPath)
        }.start()
    }

    /**
     * Descarga el Ranking Mundial desde el servidor.
     *
     * Realiza una petición GET y parsea el JSON recibido manualmente.
     *
     * @param onResultado Función callback que recibe la lista de Strings lista para mostrar.
     */
    fun obtenerRankingMundial(onResultado: (List<String>) -> Unit) {
        Thread {
            try {
                val url = URL(RANKING_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000 // 5 segundos de espera máxima
                conn.readTimeout = 5000

                if (conn.responseCode == 200) {
                    // 1. Leer la respuesta del servidor (InputStream)
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    // 2. Parsear el JSON manualmente (sin librerías como Gson)
                    // El JSON viene así: [{"nombre":"Nini", "puntos":1500}, ...]
                    val jsonArray = JSONArray(response.toString())
                    val rankingList = mutableListOf<String>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val nombre = obj.getString("nombre")
                        val puntos = obj.getInt("puntos")
                        rankingList.add("$nombre ($puntos pt)")
                    }

                    // 3. Devolver la lista procesada
                    onResultado(rankingList)

                } else {
                    onResultado(emptyList()) // Error en el servidor
                }
                conn.disconnect()

            } catch (e: Exception) {
                e.printStackTrace()
                onResultado(emptyList()) // Error de conexión o internet
            }
        }.start()
    }

    /**
     * Función interna para realizar una petición POST multipart/form-data.
     *
     * Escribe los bytes del archivo directamente en el flujo de salida de la conexión HTTP.
     *
     * @param file El archivo .db a subir.
     */
    private fun uploadFile(file: File) {
        // Configuración para el protocolo Multipart (necesario para enviar archivos)
        val boundary = "*****" + System.currentTimeMillis() + "*****"
        val lineEnd = "\r\n"
        val twoHyphens = "--"

        try {
            val fileInputStream = FileInputStream(file)
            val url = URL(UPLOAD_URL)
            val conn = url.openConnection() as HttpURLConnection

            // Configurar conexión
            conn.doInput = true
            conn.doOutput = true
            conn.useCaches = false
            conn.requestMethod = "POST"
            conn.setRequestProperty("Connection", "Keep-Alive")
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

            val dos = DataOutputStream(conn.outputStream)

            // 1. Escribir cabecera del archivo
            dos.writeBytes(twoHyphens + boundary + lineEnd)
            dos.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"DidaktikApp.db\"$lineEnd")
            dos.writeBytes(lineEnd)

            // 2. Escribir el contenido del archivo (buffer de 1KB)
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                dos.write(buffer, 0, bytesRead)
            }

            // 3. Escribir pie de página (cierre del multipart)
            dos.writeBytes(lineEnd)
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)

            // Cerrar flujos
            fileInputStream.close()
            dos.flush()
            dos.close()

            // Verificar respuesta
            val responseCode = conn.responseCode
            if (responseCode == 200 || responseCode == 201) {
                Log.d("API", "✅ ÉXITO: Base de datos subida.")
            } else {
                Log.e("API", "❌ ERROR al subir. Código: $responseCode")
            }

        } catch (e: Exception) {
            Log.e("API", "❌ EXCEPCIÓN: ${e.message}")
        }
    }
}