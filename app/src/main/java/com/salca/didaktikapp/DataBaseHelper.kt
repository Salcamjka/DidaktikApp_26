package com.salca.didaktikapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// VERSIÓN 10: Para limpiar y estabilizar
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "DidaktikApp.db", null, 10) {

    private val TABLE_ALUMNOS = "alumnos"

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

    // =================================================
    //  CREAR USUARIO (LOGIN)
    // =================================================
    fun crearUsuarioInicial(nombre: String) {
        val db = this.writableDatabase
        val nombreLimpio = nombre.trim()

        val cursor = db.rawQuery("SELECT id FROM $TABLE_ALUMNOS WHERE nombre = ?", arrayOf(nombreLimpio))
        val existe = cursor.moveToFirst()
        cursor.close()

        if (existe) return

        val values = ContentValues()
        values.put("nombre", nombreLimpio)
        db.insert(TABLE_ALUMNOS, null, values)
    }

    // =================================================
    //  GUARDAR PUNTOS
    // =================================================
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
    }

    // =================================================
    //  OBTENER PUNTOS (Lo dejamos por si acaso lo usas después)
    // =================================================
    fun obtenerPuntuacion(nombre: String, juego: String): Int {
        val db = this.readableDatabase
        val nombreLimpio = nombre.trim()

        val columna = when (juego) {
            "Ahorcado", "AhorcadoActivity" -> "ahorcado"
            "Muralla", "MurallaActivity" -> "muralla"
            "Sopa", "SopaActivity" -> "sopa"
            "Txakurra", "diferencias", "TxakurraActivity" -> "diferencias"
            "Puzzle", "PuzzleActivity" -> "puzzle"
            else -> return 0
        }

        var puntuacion = 0
        val cursor = db.rawQuery("SELECT $columna FROM $TABLE_ALUMNOS WHERE nombre = ?", arrayOf(nombreLimpio))
        if (cursor.moveToFirst()) {
            puntuacion = cursor.getInt(0)
        }
        cursor.close()

        return puntuacion
    }
}