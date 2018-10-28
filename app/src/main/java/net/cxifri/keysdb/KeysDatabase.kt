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

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import net.cxifri.crypto.KeyEntry
import java.io.Closeable


class KeysDatabase(val context: Context)
    : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_CURRENT_VERSION), Closeable {

    private var impl = KeysDatabaseImpl()

    override fun onCreate(db: SQLiteDatabase)
            = impl.createDb(db)

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

        Log.i(LOG_TAG, "onUpgrade $oldVersion -> $newVersion")

        if (oldVersion == newVersion)
            return

        if (newVersion != DATABASE_VERSION_V1)
            throw Exception("DB storage error: upgrade from $oldVersion to $newVersion is not supported")
    }

    private fun<T> implApplyToWritable(fn: KeysDatabaseImpl.(db: SQLiteDatabase) -> T) =
        synchronized(KeysDatabase::class.java) { writableDatabase.use { impl.fn(it) } }

    private fun<T> implApplyToReadable(fn: KeysDatabaseImpl.(db: SQLiteDatabase) -> T) =
        synchronized(KeysDatabase::class.java) { readableDatabase.use { impl.fn(it) } }

    fun add(key: KeyEntry) = implApplyToWritable { add(it, key) }

    fun update(key: KeyEntry) = implApplyToWritable { update(it, key) }

    fun deleteKey(keyId: Long) = implApplyToWritable { deleteKey(it, keyId) }

    fun deleteOldKeys(now: Long) = implApplyToWritable { deleteOldKeys(it, now) }

    fun getKey(keyId: Long) = implApplyToReadable { getKey(it, keyId)}

    val keys: List<KeyEntry>
        get() = implApplyToReadable { getKeys(it) }

    companion object {
        private val LOG_TAG = "KeysStorage"

        private const val DATABASE_VERSION_V1 = 1
        private const val DATABASE_CURRENT_VERSION = DATABASE_VERSION_V1

        private const val DATABASE_NAME = "keys"
    }
}
