package com.github.quarck.kriptileto

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.Closeable


class KeysDatabase(val context: Context)
    : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_CURRENT_VERSION), Closeable {

    private var impl =  KeysDatabaseImpl()

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

    val keys: List<KeyEntry>
        get() = implApplyToReadable { getKeys(it) }

    companion object {
        private val LOG_TAG = "KeysStorage"

        private const val DATABASE_VERSION_V1 = 1
        private const val DATABASE_CURRENT_VERSION = DATABASE_VERSION_V1

        private const val DATABASE_NAME = "keys"
    }
}
