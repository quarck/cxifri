/*
 * Copyright (C) 2018 Sergey Parshin (quarck@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.cxifri.keysdb

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import net.cxifri.crypto.KeyEntry
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
                        "$KEY_VALUE_TEXT TEXT, " +
                        "$KEY_VALUE_AUTH TEXT, " +
                        "$KEY_IS_ENCRYPTED INTEGER, " +
                        "$KEY_IS_REVOKED INTEGER, " +
                        "$KEY_REPLACEMENT_KEY_ID INTEGER, " +
                        "$KEY_RESERVED_INT_FIELD_1 INTEGER, " +
                        "$KEY_RESERVED_INT_FIELD_2 INTEGER, " +
                        "$KEY_RESERVED_INT_FIELD_3 INTEGER, " +
                        "$KEY_RESERVED_STR_FIELD_1 TEXT, " +
                        "$KEY_RESERVED_STR_FIELD_2 TEXT, " +
                        "$KEY_RESERVED_STR_FIELD_3 TEXT " +
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
        values.put(KEY_VALUE_TEXT, key.key)
        values.put(KEY_VALUE_AUTH, "")
        values.put(KEY_IS_ENCRYPTED, if (key.encrypted) 1 else 0)
        values.put(KEY_IS_REVOKED, if (key.revoked) 1 else 0)
        values.put(KEY_REPLACEMENT_KEY_ID, key.replacementKeyId)
        values.put(KEY_RESERVED_INT_FIELD_1, 0)
        values.put(KEY_RESERVED_INT_FIELD_2, 0)
        values.put(KEY_RESERVED_INT_FIELD_3, 0)
        values.put(KEY_RESERVED_STR_FIELD_1, "")
        values.put(KEY_RESERVED_STR_FIELD_2, "")
        values.put(KEY_RESERVED_STR_FIELD_3, "")

        return values
    }

    fun deleteKey(db: SQLiteDatabase, keyId: Long): Int {
        return db.delete(TABLE_NAME, " $KEY_ID = ?", arrayOf(keyId.toString()))
    }

    fun deleteAll(db: SQLiteDatabase): Int {
        return db.delete(TABLE_NAME, null, null)
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
                key = (cursor.getString(PROJECTION_KEY_VALUE_TEXT) as String?)
                        ?: throw Exception("Can't read key value"),
                encrypted = ((cursor.getInt(PROJECTION_KEY_IS_ENCRYPTED) as Int?)
                        ?: throw Exception("Can't get isEncrypted flag")) != 0,
                revoked = ((cursor.getInt(PROJECTION_KEY_IS_REVOKED) as Int?)
                        ?: throw Exception("Can't get isReplacementRequested flag")) != 0,
                replacementKeyId = (cursor.getLong(PROJECTION_KEY_REPLACEMENT_KEY_ID) as Long?)
                        ?: throw Exception("Can't read key replacement ID")
        )
    }

    companion object {
        private const val LOG_TAG = "KeysDatabaseImpl"

        private const val TABLE_NAME = "keys"

        private const val KEY_ID = "a"
        private const val KEY_NAME = "b"
        private const val KEY_VALUE_TEXT = "ct"
        private const val KEY_VALUE_AUTH = "ca"
        private const val KEY_IS_ENCRYPTED = "d"
        private const val KEY_IS_REVOKED = "e"
        private const val KEY_REPLACEMENT_KEY_ID = "f"
        private const val KEY_RESERVED_INT_FIELD_1 = "h"
        private const val KEY_RESERVED_INT_FIELD_2 = "i"
        private const val KEY_RESERVED_INT_FIELD_3 = "j"
        private const val KEY_RESERVED_STR_FIELD_1 = "k"
        private const val KEY_RESERVED_STR_FIELD_2 = "l"
        private const val KEY_RESERVED_STR_FIELD_3 = "m"

        private val SELECT_COLUMNS = arrayOf<String>(
                KEY_ID,
                KEY_NAME,
                KEY_VALUE_TEXT,
                KEY_VALUE_AUTH,
                KEY_IS_ENCRYPTED,
                KEY_IS_REVOKED,
                KEY_REPLACEMENT_KEY_ID
        )

        const val PROJECTION_KEY_ID = 0
        const val PROJECTION_KEY_NAME = 1
        const val PROJECTION_KEY_VALUE_TEXT = 2
        const val PROJECTION_KEY_VALUE_AUTH = 3
        const val PROJECTION_KEY_IS_ENCRYPTED = 4
        const val PROJECTION_KEY_IS_REVOKED = 5
        const val PROJECTION_KEY_REPLACEMENT_KEY_ID = 6
    }
}
