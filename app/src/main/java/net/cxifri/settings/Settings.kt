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


package net.cxifri.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import kotlin.reflect.KProperty

open class PersistentStorageBase(ctx: Context, prefName: String? = null) {
    protected var state: SharedPreferences

    init {
        state =
                if (prefName != null)
                    ctx.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                else
                    PreferenceManager.getDefaultSharedPreferences(ctx)
    }

    fun edit(): SharedPreferences.Editor {
        return state.edit()
    }

    @SuppressLint("CommitPrefEdits")
    fun setBoolean(key: String, value: Boolean) {
        val editor = state.edit()
        editor.putBoolean(key, value)
        editor.commit()
    }

    @SuppressLint("CommitPrefEdits")
    fun setInt(key: String, value: Int) {
        val editor = state.edit()
        editor.putInt(key, value)
        editor.commit()
    }

    @SuppressLint("CommitPrefEdits")
    fun setLong(key: String, value: Long) {
        val editor = state.edit()
        editor.putLong(key, value)
        editor.commit()
    }

    @SuppressLint("CommitPrefEdits")
    fun setFloat(key: String, value: Float) {
        val editor = state.edit()
        editor.putFloat(key, value)
        editor.commit()
    }

    @SuppressLint("CommitPrefEdits")
    fun setString(key: String, value: String) {
        val editor = state.edit()
        editor.putString(key, value)
        editor.commit()
    }

    @SuppressLint("CommitPrefEdits")
    fun setStringSet(key: String, value: Set<String>) {
        val editor = state.edit()
        editor.putStringSet(key, value)
        editor.commit()
    }

    fun getBoolean(key: String, default: Boolean): Boolean = state.getBoolean(key, default)

    fun getInt(key: String, default: Int): Int = state.getInt(key, default)

    fun getLong(key: String, default: Long): Long = state.getLong(key, default)

    fun getFloat(key: String, default: Float): Float = state.getFloat(key, default)

    fun getString(key: String, default: String): String = state.getString(key, default) ?: default

    fun getStringSet(key: String, default: Set<String>): Set<String> = state.getStringSet(key, default) ?: default


    class BooleanProperty(val defaultValue: Boolean, val storageName: String? = null) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
            val _this = (thisRef as PersistentStorageBase);
            val key = storageName ?: property.name
            return _this.getBoolean(key, defaultValue)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            val _this = (thisRef as PersistentStorageBase);
            val key = storageName ?: property.name
            _this.setBoolean(key, value)
        }
    }

    class IntProperty(val defaultValue: Int, val storageName: String? = null) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
            val _this = (thisRef as PersistentStorageBase);
            val key = storageName ?: property.name
            return _this.getInt(key, defaultValue)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            val _this = (thisRef as PersistentStorageBase);
            val key = storageName ?: property.name
            _this.setInt(key, value)
        }
    }

    class LongProperty(val defaultValue: Long, val storageName: String? = null) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Long {
            val _this = (thisRef as PersistentStorageBase);
            val key = storageName ?: property.name
            return _this.getLong(key, defaultValue)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
            val _this = (thisRef as PersistentStorageBase);
            val key = storageName ?: property.name
            _this.setLong(key, value)
        }
    }

    class FloatProperty(val defaultValue: Float, val storageName: String? = null) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Float {
            val _this = (thisRef as PersistentStorageBase);
            val key = storageName ?: property.name
            return _this.getFloat(key, defaultValue)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
            val _this = (thisRef as PersistentStorageBase);
            val key = storageName ?: property.name
            _this.setFloat(key, value)
        }
    }

    class StringProperty(val defaultValue: String, val storageName: String? = null) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
            val _this = (thisRef as PersistentStorageBase);
            val key = storageName ?: property.name
            return _this.getString(key, defaultValue)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            val _this = (thisRef as PersistentStorageBase);
            val key = storageName ?: property.name
            _this.setString(key, value)
        }
    }

    class StringSetProperty(val defaultValue: Set<String>, val storageName: String? = null) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Set<String> {
            val _this = (thisRef as PersistentStorageBase);
            val key = storageName ?: property.name
            return _this.getStringSet(key, defaultValue)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Set<String>) {
            val _this = (thisRef as PersistentStorageBase);
            val key = storageName ?: property.name
            _this.setStringSet(key, value)
        }
    }

}

class Settings(private val ctx: Context) : PersistentStorageBase(ctx, PREFS_NAME) {

    var useNightTheme by BooleanProperty(false, "A") // give a short name to simplify XML parsing

    companion object {
        const val PREFS_NAME: String = "persistent_state"
    }
}

val Context.settings: Settings
    get() = Settings(this)

