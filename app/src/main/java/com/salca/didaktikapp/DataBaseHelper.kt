package com.salca.didaktikapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "DidaktikApp.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE alumnos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT," +
                "puntuacion INTEGER," +
                "fecha TEXT)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS alumnos")
        onCreate(db)
    }

    // Esta función se mantiene por si la usas en el Registro, pero la importante ahora es la de abajo
    fun addStudent(nombre: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        values.put("nombre", nombre)
        values.put("puntuacion", 0)
        values.put("fecha", currentDate)

        val result = db.insert("alumnos", null, values)
        db.close()
        return result != -1L
    }

    // --- NUEVA FUNCIÓN INTELIGENTE: SI EXISTE SUMA, SI NO EXISTE CREA ---
    fun guardarPuntuacion(nombre: String, puntosNuevos: Int): Boolean {
        val db = this.writableDatabase
        var exito = false
        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // 1. Verificamos si el alumno ya existe
        val cursor = db.rawQuery("SELECT puntuacion FROM alumnos WHERE nombre = ?", arrayOf(nombre))

        if (cursor.moveToFirst()) {
            // --- CASO A: EL ALUMNO YA EXISTE -> ACTUALIZAMOS (SUMAR) ---
            val puntosAnteriores = cursor.getInt(0)
            val totalPuntos = puntosAnteriores + puntosNuevos

            val values = ContentValues()
            values.put("puntuacion", totalPuntos)
            // Opcional: Actualizar fecha de última partida
            // values.put("fecha", currentDate)

            val filas = db.update("alumnos", values, "nombre = ?", arrayOf(nombre))
            exito = filas > 0

        } else {
            // --- CASO B: EL ALUMNO NO EXISTE -> LO CREAMOS (INSERTAR) ---
            val values = ContentValues()
            values.put("nombre", nombre)
            values.put("puntuacion", puntosNuevos)
            values.put("fecha", currentDate)

            val id = db.insert("alumnos", null, values)
            exito = id != -1L
        }

        cursor.close()
        db.close()
        return exito
    }
}