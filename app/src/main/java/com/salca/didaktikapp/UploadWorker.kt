package com.salca.didaktikapp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class UploadWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val dbPath = applicationContext.getDatabasePath("DidaktikApp.db")

        if (!dbPath.exists()) {
            return Result.failure()
        }

        return try {
            // 1. TRUCO DE MAGIA: Forzamos que los datos pasen de memoria a disco
            forzarEscrituraEnDisco(dbPath.absolutePath)

            // 2. Ahora que el archivo está fresco, lo subimos
            subirArchivo(dbPath)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    // Esta función obliga a SQLite a guardar todo ANTES de enviar
    private fun forzarEscrituraEnDisco(path: String) {
        try {
            val db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE)
            // Este comando obliga a escribir los archivos temporales (.wal) en el .db principal
            val cursor = db.rawQuery("PRAGMA wal_checkpoint(FULL)", null)
            if (cursor.moveToFirst()) {
                // Checkpoint realizado
            }
            cursor.close()
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun subirArchivo(file: File) {
        val client = OkHttpClient()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "DidaktikApp.db",
                file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("https://api-didaktikapp.onrender.com/upload-db")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("Fallo en la subida: ${response.code}")
        }
    }
}