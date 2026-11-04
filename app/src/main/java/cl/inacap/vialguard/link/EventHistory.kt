package cl.inacap.vialguard.link

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class HistoryEntry(val atMillis: Long, val text: String)

/** Trazabilidad del monitor en SQLite: cambios de estado y órdenes enviadas, no cada lectura. */
class EventHistory(context: Context) : SQLiteOpenHelper(context, "vialguard_events.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE events (id INTEGER PRIMARY KEY AUTOINCREMENT, at_millis INTEGER NOT NULL, text TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun add(atMillis: Long, text: String) {
        val db = writableDatabase
        db.insert("events", null, ContentValues().apply { put("at_millis", atMillis); put("text", text) })
        db.execSQL("DELETE FROM events WHERE id NOT IN (SELECT id FROM events ORDER BY id DESC LIMIT $MAX_KEPT)")
    }

    fun recent(limit: Int = 30): List<HistoryEntry> =
        readableDatabase.rawQuery("SELECT at_millis, text FROM events ORDER BY id DESC LIMIT ?", arrayOf(limit.toString())).use { c ->
            buildList { while (c.moveToNext()) add(HistoryEntry(c.getLong(0), c.getString(1))) }
        }

    private companion object { const val MAX_KEPT = 500 }
}
