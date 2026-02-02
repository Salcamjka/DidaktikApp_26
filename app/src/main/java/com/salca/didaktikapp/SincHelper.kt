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

object SyncHelper {

    private const val BASE_URL = "https://api-didaktikapp.onrender.com"
    private const val UPLOAD_URL = "$BASE_URL/upload-db"
    private const val RANKING_URL = "$BASE_URL/ranking"

    // --- FUNCIÓN PARA SUBIR DATOS (POST) ---
    fun subirInmediatamente(context: Context) {
        Thread {
            val dbPath = context.getDatabasePath("DidaktikApp.db")
            if (!dbPath.exists()) return@Thread

            try {
                // Checkpoint para asegurar datos frescos
                val dbHelper = DatabaseHelper(context)
                val db = dbHelper.writableDatabase
                db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).close()
                db.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            uploadFile(dbPath)
        }.start()
    }

    // --- NUEVA FUNCIÓN PARA DESCARGAR RANKING (GET) ---
    // Recibe una "función callback" para devolver los datos cuando lleguen
    fun obtenerRankingMundial(onResultado: (List<String>) -> Unit) {
        Thread {
            try {
                val url = URL(RANKING_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode == 200) {
                    // Leer respuesta
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    // Parsear JSON manual (Sin Retrofit)
                    // El JSON viene así: [{"nombre":"Nini", "puntos":1500}, ...]
                    val jsonArray = JSONArray(response.toString())
                    val rankingList = mutableListOf<String>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val nombre = obj.getString("nombre")
                        val puntos = obj.getInt("puntos")
                        rankingList.add("$nombre ($puntos pt)")
                    }

                    // Devolver lista al hilo principal
                    onResultado(rankingList)

                } else {
                    onResultado(emptyList()) // Error o lista vacía
                }
                conn.disconnect()

            } catch (e: Exception) {
                e.printStackTrace()
                onResultado(emptyList()) // Error de conexión
            }
        }.start()
    }

    // --- LÓGICA INTERNA DE SUBIDA ---
    private fun uploadFile(file: File) {
        val boundary = "*****" + System.currentTimeMillis() + "*****"
        val lineEnd = "\r\n"
        val twoHyphens = "--"

        try {
            val fileInputStream = FileInputStream(file)
            val url = URL(UPLOAD_URL)
            val conn = url.openConnection() as HttpURLConnection

            conn.doInput = true
            conn.doOutput = true
            conn.useCaches = false
            conn.requestMethod = "POST"
            conn.setRequestProperty("Connection", "Keep-Alive")
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

            val dos = DataOutputStream(conn.outputStream)

            dos.writeBytes(twoHyphens + boundary + lineEnd)
            dos.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"DidaktikApp.db\"$lineEnd")
            dos.writeBytes(lineEnd)

            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                dos.write(buffer, 0, bytesRead)
            }

            dos.writeBytes(lineEnd)
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)

            fileInputStream.close()
            dos.flush()
            dos.close()

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