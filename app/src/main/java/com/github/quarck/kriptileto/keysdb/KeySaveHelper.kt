package com.github.quarck.kriptileto.keysdb

import android.content.Context
import android.os.Build
import com.github.quarck.kriptileto.aks.AndroidKeyStore
import org.bouncycastle.util.encoders.UrlBase64

class KeySaveHelper {

    fun saveKey(context: Context, name: String, key: ByteArray, preferAndroidKeyStore: Boolean) {
        KeysDatabase(context).use {
            db ->

            val id = db.add(KeyEntry.forName("_")) // temp name to make sure it was updated

            val updatedKeyEntry =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && preferAndroidKeyStore) {
                        val aks = AndroidKeyStore()
                        aks.createKey(id) // create matching keystore key that would be encrypting this key in DB
                        val encryptedKey = aks.encrypt(id, key)
                        val encryptedBase64Key = UrlBase64.encode(encryptedKey)
                        KeyEntry(id, name, encryptedBase64Key.toString(charset = Charsets.UTF_8), true)
                    }
                    else {
                        val base64Key = UrlBase64.encode(key)
                        KeyEntry(id, name, base64Key.toString(charset = Charsets.UTF_8), false)
                    }

            db.update(updatedKeyEntry)
        }
    }

}