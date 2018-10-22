package com.github.quarck.kriptileto.keysdb

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.util.*

class KeysDatabaseImpl {

    @Suppress("ConvertToStringTemplate")
    fun createDb(db: SQLiteDatabase) {

        val CREATE_PKG_TABLE =
                "CREATE " +
                        "TABLE $TABLE_NAME " +
                        "( " +
                        "$KEY_ID INTEGER PRIMARY KEY, " +
                        "$KEY_NAME TEXT, " +
                        "$KEY_VALUE TEXT, " +
                        "$KEY_IS_ENCRYPTED INTEGER, " +
                        "$KEY_IS_REPLACEMENT_REQUESTED INTEGER, " +
                        "$KEY_REPLACEMENT_KEY_ID INTEGER, " +
                        "$KEY_DELETE_AFTER INTEGER" +
                        " )"

        Log.d(LOG_TAG, "Creating DB TABLE using query: " + CREATE_PKG_TABLE)

        db.execSQL(CREATE_PKG_TABLE)
    }

    fun dropTables(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME)
    }

    fun add(db: SQLiteDatabase, key: KeyEntry): Long {
        return db.insert(TABLE_NAME, null, encodeContentValues(key))
    }

    fun update(db: SQLiteDatabase, key: KeyEntry): Boolean {

        return db.update(
                TABLE_NAME,
                encodeContentValues(key),
                "$KEY_ID = ?",
                arrayOf(key.id.toString())
        ) == 1
    }

    private fun encodeContentValues(key: KeyEntry): ContentValues {
        val values = ContentValues()

        values.put(KEY_NAME, key.name)
        values.put(KEY_VALUE, key.value)
        values.put(KEY_IS_ENCRYPTED, if (key.encrypted) 1 else 0)
        values.put(KEY_IS_REPLACEMENT_REQUESTED, if (key.replaceRequested) 1 else 0)
        values.put(KEY_REPLACEMENT_KEY_ID, key.replacementKeyId)
        values.put(KEY_DELETE_AFTER, key.deleteAfter)

        return values
    }

    fun deleteKey(db: SQLiteDatabase, keyId: Long): Int {
        return db.delete(TABLE_NAME, " $KEY_ID = ?", arrayOf(keyId.toString()))
    }

    fun deleteOldKeys(db: SQLiteDatabase, now: Long): Int {
        return db.delete(TABLE_NAME,
                " $KEY_DELETE_AFTER > 0 AND $KEY_DELETE_AFTER < ?", arrayOf(now.toString()))
    }

    fun getKeys(db: SQLiteDatabase): List<KeyEntry> {

        val ret = LinkedList<KeyEntry>()

        val cursor = db.query(TABLE_NAME, // a. table
                SELECT_COLUMNS, // b. column names
                null, // c. selections
                null,
                null, // e. group by
                null, // f. h aving
                null, // g. order by
                null) // h. limit

        if (cursor.moveToFirst()) {
            do {
                ret.add(cursorToKey(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()

        Log.d(LOG_TAG, "eventsImpl, returning ${ret.size} requests")

        return ret
    }

    fun getKey(db: SQLiteDatabase, keyId: Long): KeyEntry? {

        var ret: KeyEntry? = null

        val cursor = db.query(TABLE_NAME, // a. table
                SELECT_COLUMNS, // b. column names
                " $KEY_ID = ?", // c. selections
                arrayOf(keyId.toString()),
                null, // e. group by
                null, // f. h aving
                null, // g. order by
                null) // h. limit

        if (cursor.moveToFirst()) {
            do {
                ret = cursorToKey(cursor)
            } while (cursor.moveToNext())
        }
        cursor.close()

        return ret
    }

    private fun cursorToKey(cursor: Cursor): KeyEntry {

        return KeyEntry(
                id = (cursor.getLong(PROJECTION_KEY_ID) as Long?)
                        ?: throw Exception("Can't read key ID"),
                name = (cursor.getString(PROJECTION_KEY_NAME) as String?)
                        ?: throw Exception("Can't read key name"),
                value = (cursor.getString(PROJECTION_KEY_VALUE) as String?)
                        ?: throw Exception("Can't read key value"),
                encrypted = ((cursor.getInt(PROJECTION_KEY_IS_ENCRYPTED) as Int?)
                        ?: throw Exception("Can't get isEncrypted flag")) != 0,
                replaceRequested = ((cursor.getInt(PROJECTION_KEY_IS_REPLACEMENT_REQUESTED) as Int?)
                        ?: throw Exception("Can't get isReplacementRequested flag")) != 0,
                replacementKeyId = (cursor.getLong(PROJECTION_KEY_REPLACEMENT_KEY_ID) as Long?)
                        ?: throw Exception("Can't read key replacement ID"),
                deleteAfter = (cursor.getLong(PROJECTION_KEY_DELETE_AFTER) as Long?)
                        ?: throw Exception("Can't read key deleteAfter")
        )
    }

    companion object {
        private const val LOG_TAG = "KeysDatabaseImpl"

        private const val TABLE_NAME = "keys"

        private const val KEY_ID = "a"
        private const val KEY_NAME = "b"
        private const val KEY_VALUE = "c"
        private const val KEY_IS_ENCRYPTED = "d"
        private const val KEY_IS_REPLACEMENT_REQUESTED = "e"
        private const val KEY_REPLACEMENT_KEY_ID = "f"
        private const val KEY_DELETE_AFTER = "h"

        private val SELECT_COLUMNS = arrayOf<String>(
                KEY_ID,
                KEY_NAME,
                KEY_VALUE,
                KEY_IS_ENCRYPTED,
                KEY_IS_REPLACEMENT_REQUESTED,
                KEY_REPLACEMENT_KEY_ID,
                KEY_DELETE_AFTER
        )

        const val PROJECTION_KEY_ID = 0
        const val PROJECTION_KEY_NAME = 1
        const val PROJECTION_KEY_VALUE = 2
        const val PROJECTION_KEY_IS_ENCRYPTED = 3

        const val PROJECTION_KEY_IS_REPLACEMENT_REQUESTED = 4
        const val PROJECTION_KEY_REPLACEMENT_KEY_ID = 5
        const val PROJECTION_KEY_DELETE_AFTER = 6

    }
}
