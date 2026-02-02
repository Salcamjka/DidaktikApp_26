package com.salca.didaktikapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "DidaktikApp.db", null, 10) {

    private val TABLE_ALUMNOS = "alumnos"

    // 👇 ESTO ES LO QUE ARREGLA LA SUBIDA DE ARCHIVOS
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.disableWriteAheadLogging()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_ALUMNOS (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT UNIQUE COLLATE NOCASE, " +
                "ahorcado INTEGER DEFAULT 0, " +
                "muralla INTEGER DEFAULT 0, " +
                "sopa INTEGER DEFAULT 0, " +
                "diferencias INTEGER DEFAULT 0, " +
                "puzzle INTEGER DEFAULT 0)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ALUMNOS")
        db.execSQL("DROP TABLE IF EXISTS puntuaciones_pendientes")
        db.execSQL("DROP TABLE IF EXISTS puntuaciones")
        db.execSQL("DROP TABLE IF EXISTS usuarios")
        onCreate(db)
    }

    fun crearUsuarioInicial(nombre: String) {
        val db = this.writableDatabase
        val nombreLimpio = nombre.trim()

        val cursor = db.rawQuery("SELECT id FROM $TABLE_ALUMNOS WHERE nombre = ?", arrayOf(nombreLimpio))
        val existe = cursor.moveToFirst()
        cursor.close()

        if (!existe) {
            val values = ContentValues()
            values.put("nombre", nombreLimpio)
            db.insert(TABLE_ALUMNOS, null, values)
        }
        db.close()
    }

    fun guardarPuntuacion(nombre: String, juego: String, puntos: Int) {
        val db = this.writableDatabase
        val nombreLimpio = nombre.trim()

        val columna = when (juego) {
            "Ahorcado", "AhorcadoActivity" -> "ahorcado"
            "Muralla", "MurallaActivity" -> "muralla"
            "Sopa", "SopaActivity" -> "sopa"
            "Txakurra", "diferencias", "TxakurraActivity" -> "diferencias"
            "Puzzle", "PuzzleActivity" -> "puzzle"
            else -> return
        }

        val values = ContentValues()
        values.put(columna, puntos)
        db.update(TABLE_ALUMNOS, values, "nombre = ?", arrayOf(nombreLimpio))
        db.close()
    }

    fun obtenerPuntuacion(nombre: String, juego: String): Int {
        val db = this.readableDatabase
        val nombreLimpio = nombre.trim()

        // Mapeo simple de nombres de actividad a columnas
        val columna = when {
            juego.contains("Ahorcado") -> "ahorcado"
            juego.contains("Muralla") -> "muralla"
            juego.contains("Sopa") -> "sopa"
            juego.contains("Txakurra") || juego.contains("diferencias") -> "diferencias"
            juego.contains("Puzzle") -> "puzzle"
            else -> return 0
        }

        var puntuacion = 0
        val cursor = db.rawQuery("SELECT $columna FROM $TABLE_ALUMNOS WHERE nombre = ?", arrayOf(nombreLimpio))
        if (cursor.moveToFirst()) {
            puntuacion = cursor.getInt(0)
        }
        cursor.close()
        db.close()
        return puntuacion
    }

    fun getTop3Ranking(): List<String> {
        val rankingList = mutableListOf<String>()
        val db = this.readableDatabase

        val query = """
            SELECT nombre, (ahorcado + muralla + sopa + diferencias + puzzle) as total 
            FROM $TABLE_ALUMNOS 
            ORDER BY total DESC 
            LIMIT 3
        """

        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                val nombre = cursor.getString(0)
                val puntos = cursor.getInt(1)
                rankingList.add("$nombre ($puntos pt)")
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return rankingList
    }

    // 👇 FUNCIÓN NUEVA PARA BORRAR Y REINICIAR ID A 1
    fun borrarTodo() {
        val db = this.writableDatabase

        // 1. Borra todos los alumnos
        db.execSQL("DELETE FROM $TABLE_ALUMNOS")

        // 2. Reinicia el contador (ID vuelve a 1)
        // OJO: 'sqlite_sequence' es una tabla interna de Android, no la toques
        db.execSQL("DELETE FROM sqlite_sequence WHERE name = '$TABLE_ALUMNOS'")

        db.close()
    }
}