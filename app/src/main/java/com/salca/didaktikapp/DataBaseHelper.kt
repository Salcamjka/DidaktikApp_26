package com.salca.didaktikapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// VERSIÓN 7: Limpieza total, solo queda la tabla de alumnos
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "DidaktikApp.db", null, 7) {

    private val TABLE_ALUMNOS = "alumnos"

    override fun onCreate(db: SQLiteDatabase) {
        // Creamos la tabla EXACTAMENTE como la espera tu servidor
        // Solo guardamos los totales de cada juego por alumno
        db.execSQL("CREATE TABLE $TABLE_ALUMNOS (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT UNIQUE, " +
                "ahorcado INTEGER DEFAULT 0, " +
                "muralla INTEGER DEFAULT 0, " +
                "sopa INTEGER DEFAULT 0, " +
                "diferencias INTEGER DEFAULT 0, " +
                "puzzle INTEGER DEFAULT 0)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Borramos todo lo anterior para empezar limpio
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ALUMNOS")
        db.execSQL("DROP TABLE IF EXISTS puntuaciones_pendientes") // Borramos la que no quieres
        db.execSQL("DROP TABLE IF EXISTS puntuaciones")
        db.execSQL("DROP TABLE IF EXISTS usuarios")
        onCreate(db)
    }

    // =================================================
    //  LOGIN: Crear usuario si no existe
    // =================================================
    fun crearUsuarioInicial(nombre: String) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put("nombre", nombre)
        // Si ya existe, lo ignora (no duplica, no falla)
        db.insertWithOnConflict(TABLE_ALUMNOS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    // =================================================
    //  GUARDAR PUNTOS (Actualiza la columna del alumno)
    // =================================================
    fun guardarPuntuacion(nombre: String, juego: String, puntos: Int) {
        val db = this.writableDatabase

        // Traducimos tus nombres de Activity a las columnas de la BD
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

        // Actualizamos la fila de este alumno con la nueva puntuación
        db.update(TABLE_ALUMNOS, values, "nombre = ?", arrayOf(nombre))
    }
}