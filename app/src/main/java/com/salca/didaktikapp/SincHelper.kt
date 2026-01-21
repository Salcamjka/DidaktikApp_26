package com.salca.didaktikapp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody

object SyncHelper {
    fun subirInmediatamente(context: Context) {
        val dbPath = context.getDatabasePath("DidaktikApp.db")
        if (!dbPath.exists()) return

        // 1. Checkpoint (Guardar todo a disco)
        try {
            val db = SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
            db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).close()
            db.close()
        } catch (e: Exception) { e.printStackTrace() }

        // 2. Subir a la nube en segundo plano
        Thread {
            try {
                val client = OkHttpClient()
                val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", "DidaktikApp.db", dbPath.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
                    .build()
                val request = Request.Builder().url("https://api-didaktikapp.onrender.com/upload-db").post(requestBody).build()
                client.newCall(request).execute()
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }
}