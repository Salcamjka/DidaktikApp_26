package com.salca.didaktikapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Clase auxiliar para la gestión de la base de datos SQLite local.
 *
 * Esta clase se encarga de:
 * * Crear y actualizar la estructura de la tabla de alumnos.
 * * Gestionar el registro de nuevos usuarios.
 * * Guardar y actualizar las puntuaciones de los 5 juegos.
 * * Calcular el Ranking local (Top 3).
 *
 * @property TABLE_ALUMNOS Nombre de la tabla principal ("alumnos").
 * @constructor Crea un helper para la base de datos "DidaktikApp.db" (versión 10).
 * @author Nizam
 * @version 1.0
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "DidaktikApp.db", null, 10) {

    private val TABLE_ALUMNOS = "alumnos"

    /**
     * Configuración previa a la apertura de la base de datos.
     *
     * Se deshabilita el "Write-Ahead Logging" (WAL) para asegurar que el archivo .db
     * se actualice inmediatamente en el disco y no dé problemas al subirlo al servidor.
     */
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.disableWriteAheadLogging()
    }

    /**
     * Método llamado cuando la base de datos se crea por primera vez.
     *
     * Crea la tabla 'alumnos' con las columnas para el nombre y las puntuaciones
     * de cada uno de los 5 juegos.
     */
    override fun onCreate(db: SQLiteDatabase) {
        // SQL: ID autoincremental, Nombre único (sin distinguir mayúsculas) y 5 juegos iniciados a 0.
        db.execSQL("CREATE TABLE $TABLE_ALUMNOS (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT UNIQUE COLLATE NOCASE, " +
                "ahorcado INTEGER DEFAULT 0, " +
                "muralla INTEGER DEFAULT 0, " +
                "sopa INTEGER DEFAULT 0, " +
                "diferencias INTEGER DEFAULT 0, " +
                "puzzle INTEGER DEFAULT 0)")
    }

    /**
     * Método llamado cuando se detecta un cambio de versión en la base de datos.
     *
     * Borra las tablas antiguas y vuelve a crearlas para asegurar la estructura correcta.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ALUMNOS")
        // Limpieza de tablas antiguas o temporales que ya no se usan
        db.execSQL("DROP TABLE IF EXISTS puntuaciones_pendientes")
        db.execSQL("DROP TABLE IF EXISTS puntuaciones")
        db.execSQL("DROP TABLE IF EXISTS usuarios")
        onCreate(db)
    }

    /**
     * Registra un nuevo alumno en la base de datos si no existe previamente.
     *
     * @param nombre El nombre o nick del alumno.
     */
    fun crearUsuarioInicial(nombre: String) {
        val db = this.writableDatabase
        val nombreLimpio = nombre.trim()

        // 1. Comprobamos si ya existe alguien con ese nombre
        val cursor = db.rawQuery("SELECT id FROM $TABLE_ALUMNOS WHERE nombre = ?", arrayOf(nombreLimpio))
        val existe = cursor.moveToFirst()
        cursor.close()

        // 2. Si no existe, lo insertamos
        if (!existe) {
            val values = ContentValues()
            values.put("nombre", nombreLimpio)
            db.insert(TABLE_ALUMNOS, null, values)
        }
        db.close()
    }

    /**
     * Actualiza la puntuación de un juego específico para un alumno.
     *
     * @param nombre Nombre del alumno.
     * @param juego Nombre de la actividad (ej: "Ahorcado", "MurallaActivity").
     * @param puntos Puntuación a guardar.
     */
    fun guardarPuntuacion(nombre: String, juego: String, puntos: Int) {
        val db = this.writableDatabase
        val nombreLimpio = nombre.trim()

        // Mapeo del nombre de la actividad al nombre de la columna en la BD
        val columna = when (juego) {
            "Ahorcado", "AhorcadoActivity" -> "ahorcado"
            "Muralla", "MurallaActivity" -> "muralla"
            "Sopa", "SopaActivity" -> "sopa"
            "Txakurra", "diferencias", "TxakurraActivity" -> "diferencias"
            "Puzzle", "PuzzleActivity" -> "puzzle"
            else -> return // Si el juego no coincide, salimos
        }

        val values = ContentValues()
        values.put(columna, puntos)

        // Ejecutamos la actualización (UPDATE) filtrando por nombre
        db.update(TABLE_ALUMNOS, values, "nombre = ?", arrayOf(nombreLimpio))
        db.close()
    }

    /**
     * Obtiene la puntuación actual de un alumno en un juego concreto.
     *
     * @return Puntos guardados (Int). Devuelve 0 si no encuentra el usuario.
     */
    fun obtenerPuntuacion(nombre: String, juego: String): Int {
        val db = this.readableDatabase
        val nombreLimpio = nombre.trim()

        // Determinar qué columna leer
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

    /**
     * Calcula y devuelve el Ranking local (Top 3).
     *
     * Suma las puntuaciones de los 5 juegos para cada usuario y los ordena de mayor a menor.
     *
     * @return Lista de Strings con el formato "Nombre (Puntos pt)".
     */
    fun getTop3Ranking(): List<String> {
        val rankingList = mutableListOf<String>()
        val db = this.readableDatabase

        // SQL: Sumamos columnas, ordenamos descendente y cogemos los 3 primeros
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

    /**
     * Elimina todos los datos de la tabla de alumnos.
     *
     * Útil para reiniciar la aplicación o limpiar datos en un entorno de pruebas.
     * También reinicia el contador autoincremental de IDs.
     */
    fun borrarTodo() {
        val db = this.writableDatabase

        // 1. Borra todas las filas
        db.execSQL("DELETE FROM $TABLE_ALUMNOS")

        // 2. Reinicia el contador de IDs (tabla interna sqlite_sequence)
        db.execSQL("DELETE FROM sqlite_sequence WHERE name = '$TABLE_ALUMNOS'")

        db.close()
    }
}