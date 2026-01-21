package com.salca.didaktikapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "DidaktikApp.db", null, 1) {

    // Se crea la tabla solo si no existe
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS alumnos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT, " +
                "ahorcado INTEGER DEFAULT 0, " +
                "muralla INTEGER DEFAULT 0, " +
                "sopa INTEGER DEFAULT 0, " +
                "diferencias INTEGER DEFAULT 0, " +
                "puzzle INTEGER DEFAULT 0)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS alumnos")
        onCreate(db)
    }

    // ==========================================
    // 🧠 LÓGICA INTELIGENTE: NO REPETIR USUARIOS
    // ==========================================
    fun crearUsuarioInicial(nombre: String) {
        val db = this.writableDatabase

        // 1. PRIMERO MIRAMOS SI YA EXISTE
        // Hacemos una consulta buscando ese nombre
        val cursor = db.rawQuery("SELECT * FROM alumnos WHERE nombre = ?", arrayOf(nombre))

        if (cursor.moveToFirst()) {
            // 🛑 EL USUARIO YA EXISTE
            // No hacemos nada. Así mantenemos su ID y sus puntos antiguos.
            // (Si quisieras actualizar algo, lo harías aquí)
        } else {
            // ✅ EL USUARIO NO EXISTE (ES NUEVO)
            // Creamos una fila nueva y se le asignará un ID nuevo automáticamente.
            val values = ContentValues()
            values.put("nombre", nombre)
            values.put("ahorcado", 0)
            values.put("muralla", 0)
            values.put("sopa", 0)
            values.put("diferencias", 0)
            values.put("puzzle", 0)

            db.insert("alumnos", null, values)
        }

        cursor.close()
        db.close()
    }

    // Función para guardar/sumar puntos
    fun guardarPuntuacion(nombre: String, juego: String, puntos: Int) {
        val db = this.writableDatabase

        // Traducimos el nombre de tu actividad al nombre de la columna en la BD
        val columna = when (juego) {
            "Ahorcado" -> "ahorcado"
            "Muralla" -> "muralla"
            "Sopa" -> "sopa"
            "Txakurra", "TxakurraTabla" -> "diferencias" // Asumimos que Txakurra va a 'diferencias'
            "Puzzle" -> "puzzle"
            else -> return // Si no coincide, salimos
        }

        // 1. Buscamos los puntos que ya tenía para SUMARLOS (opcional)
        // Si prefieres que se sobrescriba (que 100 borre a 50), quita esta parte de lectura.
        var puntosAnteriores = 0
        val cursor = db.rawQuery("SELECT $columna FROM alumnos WHERE nombre = ?", arrayOf(nombre))
        if (cursor.moveToFirst()) {
            puntosAnteriores = cursor.getInt(0)
        }
        cursor.close()

        // 2. Si quieres que se SUMEN a lo que ya tenía:
        // val puntosFinales = puntosAnteriores + puntos

        // 3. Si quieres guardar la puntuación ACTUAL (reemplazando la vieja):
        val puntosFinales = puntos

        // (Elige la opción 2 o 3 según prefieras. Ahora mismo está puesto REEMPLAZAR).

        val values = ContentValues()
        values.put(columna, puntosFinales)

        // Actualizamos solo la fila de este alumno
        db.update("alumnos", values, "nombre = ?", arrayOf(nombre))
        db.close()
    }

    // Función auxiliar para sumar puntos directamente (útil para juegos acumulativos)
    fun sumarPuntos(juego: String, puntos: Int) {
        // Esta función requeriría pasarle el nombre también,
        // pero puedes usar guardarPuntuacion con la lógica de suma.
    }
}