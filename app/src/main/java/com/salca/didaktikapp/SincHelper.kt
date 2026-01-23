package com.salca.didaktikapp

import android.content.Context
import android.util.Log
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

object SyncHelper {

    // ✅ CORREGIDO: Apunta a tu ordenador local en lugar de a la nube
    // Nota: Si mañana tu IP cambia, tendrás que actualizar este número.
    private const val API_URL = "https://api-didaktikapp.onrender.com/upload-db0"

    fun subirInmediatamente(context: Context) {
        Thread {
            val dbPath = context.getDatabasePath("DidaktikApp.db")

            // Si no existe la base de datos, no hacemos nada
            if (!dbPath.exists()) return@Thread

            // 1. FORZAR GUARDADO: Obligamos a Android a escribir los datos en el archivo .db
            try {
                val dbHelper = DatabaseHelper(context)
                val db = dbHelper.writableDatabase
                db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).close()
                // No cerramos la conexión para no interferir con la app
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. SUBIR EL ARCHIVO AL SERVIDOR
            uploadFile(dbPath)
        }.start()
    }

    private fun uploadFile(file: File) {
        val boundary = "*****" + System.currentTimeMillis() + "*****"
        val lineEnd = "\r\n"
        val twoHyphens = "--"

        try {
            val fileInputStream = FileInputStream(file)
            val url = URL(API_URL)
            val conn = url.openConnection() as HttpURLConnection

            // Configuración de la conexión HTTP
            conn.doInput = true
            conn.doOutput = true
            conn.useCaches = false
            conn.requestMethod = "POST"
            conn.setRequestProperty("Connection", "Keep-Alive")
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

            val dos = DataOutputStream(conn.outputStream)

            // Escribir cabeceras del formulario (campo "file")
            dos.writeBytes(twoHyphens + boundary + lineEnd)
            dos.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"DidaktikApp.db\"$lineEnd")
            dos.writeBytes(lineEnd)

            // Leer el archivo y enviarlo
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

            // Verificar respuesta
            val responseCode = conn.responseCode
            if (responseCode == 200 || responseCode == 201) {
                Log.d("API", "✅ ÉXITO: Base de datos subida al PC. Código: $responseCode")
            } else {
                Log.e("API", "❌ ERROR al subir. Código: $responseCode")
            }

        } catch (e: Exception) {
            Log.e("API", "❌ EXCEPCIÓN DE CONEXIÓN: ${e.message}")
            e.printStackTrace()
        }
    }
}