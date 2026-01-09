package com.salca.didaktikapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Heredamos de SQLiteOpenHelper para gestionar la base de datos
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "DidaktikApp.db", null, 1) {

    // Se llama la primera vez para crear la tabla
    override fun onCreate(db: SQLiteDatabase) {
        // Creamos una tabla llamada 'alumnos' con id, nombre y fecha
        val createTable = "CREATE TABLE alumnos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nombre TEXT," +
                "fecha TEXT)"
        db.execSQL(createTable)
    }

    // Se llama si cambiamos la versión de la DB (no lo usaremos por ahora)
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS alumnos")
        onCreate(db)
    }

    // MÉTODO PARA GUARDAR UN ALUMNO
    fun addStudent(nombre: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        // Obtenemos la fecha y hora actual automáticamente
        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        values.put("nombre", nombre)
        values.put("fecha", currentDate)

        // Insertamos y cerramos
        val result = db.insert("alumnos", null, values)
        db.close()

        // Si result es -1 hubo un error, si no, todo bien
        return result != -1L
    }
}