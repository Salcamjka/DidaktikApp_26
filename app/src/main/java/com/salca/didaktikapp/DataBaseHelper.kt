package com.salca.didaktikapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// ⚠️ CAMBIO IMPORTANTE: Versión subida a 8 para aplicar la corrección
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "DidaktikApp.db", null, 8) {

    private val TABLE_ALUMNOS = "alumnos"

    override fun onCreate(db: SQLiteDatabase) {
        // AÑADIDO 'COLLATE NOCASE': Para que "Pepe" y "pepe" cuenten como el mismo usuario
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
        // Borrón y cuenta nueva para asegurar que la estructura es perfecta
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ALUMNOS")
        db.execSQL("DROP TABLE IF EXISTS puntuaciones_pendientes")
        db.execSQL("DROP TABLE IF EXISTS puntuaciones")
        db.execSQL("DROP TABLE IF EXISTS usuarios")
        onCreate(db)
    }

    // =================================================
    //  LOGIN: CORREGIDO PARA NO DUPLICAR ID
    // =================================================
    fun crearUsuarioInicial(nombre: String) {
        val db = this.writableDatabase

        // 1. Limpieza: Quitamos espacios accidentales (ej: "Mikel " -> "Mikel")
        val nombreLimpio = nombre.trim()

        // 2. Comprobación Manual: ¿Existe ya este nombre?
        val cursor = db.rawQuery("SELECT id FROM $TABLE_ALUMNOS WHERE nombre = ?", arrayOf(nombreLimpio))
        val existe = cursor.moveToFirst() // Devuelve true si encuentra al usuario
        cursor.close()

        if (existe) {
            // ✅ YA EXISTE: No hacemos nada. Se usará el ID antiguo.
            return
        }

        // 3. NO EXISTE: Creamos uno nuevo (ID nuevo)
        val values = ContentValues()
        values.put("nombre", nombreLimpio)

        // Usamos insert normal porque ya hemos comprobado manualmente
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
            "Txakurra", "TxakurraTabla", "TxakurraActivity" -> "diferencias"
            "Puzzle", "PuzzleActivity" -> "puzzle"
            else -> return
        }

        val values = ContentValues()
        values.put(columna, puntos)

        // Actualizamos buscando por el nombre limpio
        db.update(TABLE_ALUMNOS, values, "nombre = ?", arrayOf(nombreLimpio))
    }
}