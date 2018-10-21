package com.github.quarck.kriptileto.ui

import android.os.Bundle
import android.app.Activity
import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import com.github.quarck.kriptileto.R
import com.github.quarck.kriptileto.aks.AndroidKeyStore
import com.github.quarck.kriptileto.crypto.AESTwofishSerpentEngine
import com.github.quarck.kriptileto.crypto.RandomKeyGenerator
import com.github.quarck.kriptileto.dataprocessing.QREncoder
import com.github.quarck.kriptileto.keysdb.KeyEntry
import com.github.quarck.kriptileto.keysdb.KeysDatabase

import kotlinx.android.synthetic.main.activity_random_key_generation.*
import org.bouncycastle.util.encoders.UrlBase64
import org.bouncycastle.util.encoders.UrlBase64Encoder

class RandomKeyGenerationActivity : Activity() {

    lateinit var key: ByteArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_random_key_generation)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        val imageView = findViewById<ImageView>(R.id.imageViewQR)

        val gen = QREncoder(512)

        val keyGen = RandomKeyGenerator()
        key = keyGen.generate(AESTwofishSerpentEngine.KEY_LENGTH_BYTES)
        val base64key = UrlBase64.encode(key).toString(charset = Charsets.UTF_8)

        val img = gen.encodeAsBitmap(base64key)

        imageView.setImageBitmap(img)

        findViewById<Button>(R.id.buttonSave).setOnClickListener(this::onButtonSave)
    }

    private fun onButtonSave(v: View) {
        val name = findViewById<EditText>(R.id.editTextKeyName)

        name?.text?.toString()?.let {
            saveKey(it)
            finish()
        }
    }

    private fun saveKey(name: String) {
        KeysDatabase(context = this).use {
            db ->

            val id = db.add(KeyEntry.forName("_")) // temp name to make sure it was updated

            val updatedKeyEntry =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val aks = AndroidKeyStore()
                        aks.createKey(id) // create matchng keystore key that would be encrypting this key in DB
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
