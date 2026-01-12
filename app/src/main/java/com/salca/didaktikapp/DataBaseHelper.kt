package com.salca.didaktikapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// VERSIÓN 8: Estructura final con tus 5 actividades
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "DidaktikApp.db", null, 8) {

    override fun onCreate(db: SQLiteDatabase) {
        // CREAMOS LAS 5 COLUMNAS PARA TUS JUEGOS
        val createTable = "CREATE TABLE alumnos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT UNIQUE," +
                "ahorcado INTEGER DEFAULT 0," +
                "muralla INTEGER DEFAULT 0," +
                "sopa INTEGER DEFAULT 0," +
                "diferencias INTEGER DEFAULT 0," +
                "puzzle INTEGER DEFAULT 0)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS alumnos")
        onCreate(db)
    }

    // --- FUNCIÓN CEREBRO: Decide en qué columna guardar ---
    fun guardarPuntuacion(nombre: String, actividad: String, puntos: Int): Boolean {
        val db = this.writableDatabase
        var exito = false

        // 1. ELEGIMOS LA COLUMNA SEGÚN EL NOMBRE DEL JUEGO
        val columnaDestino = when (actividad) {
            "Ahorcado"    -> "ahorcado"
            "Muralla"     -> "muralla"
            "Sopa"        -> "sopa"
            "Diferencias" -> "diferencias"
            "Puzzle"      -> "puzzle"
            else          -> null // Si el nombre está mal escrito, no hace nada
        }

        if (columnaDestino == null) return false

        try {
            // 2. Comprobamos si el alumno existe
            val cursor = db.rawQuery("SELECT id FROM alumnos WHERE nombre = ?", arrayOf(nombre))
            val existe = cursor.moveToFirst()
            cursor.close()

            val values = ContentValues()
            values.put(columnaDestino, puntos) // Guardamos puntos en la columna correcta

            if (existe) {
                // Si existe, actualizamos solo esa columna
                val filas = db.update("alumnos", values, "nombre = ?", arrayOf(nombre))
                exito = filas > 0
            } else {
                // Si no existe, lo creamos
                values.put("nombre", nombre)
                val id = db.insert("alumnos", null, values)
                exito = id != -1L
            }

        } catch (e: Exception) {
            exito = false
        } finally {
            db.close()
        }

        return exito
    }

    // Función para crear al usuario al hacer Login (todo a 0)
    fun crearUsuarioInicial(nombre: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put("nombre", nombre)

        return try {
            db.insertWithOnConflict("alumnos", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            db.close()
            true
        } catch (e: Exception) {
            db.close()
            false
        }
    }
}