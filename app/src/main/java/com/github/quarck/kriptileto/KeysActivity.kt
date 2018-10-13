package com.github.quarck.kriptileto

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import org.bouncycastle.crypto.CryptoException
import org.bouncycastle.util.encoders.UrlBase64

class KeysActivity : Activity() {
    lateinit var keysRoot: LinearLayout
    lateinit var addNewKeyButton: Button
    lateinit var addKeyLayout: LinearLayout
    lateinit var keyName: EditText
    lateinit var keyPassword: EditText
    lateinit var keyPasswordConfirmation: EditText
    lateinit var buttonSaveKey: Button
    lateinit var buttonCancel: Button
    lateinit var textError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keys)

        keysRoot = findViewById(R.id.layoutExistingKeysRoot) ?: throw Exception("Layout error")
        addNewKeyButton = findViewById(R.id.buttonAddNewKey) ?: throw Exception("Layout error")
        addKeyLayout = findViewById(R.id.layoutAddKey) ?: throw Exception("Layout error")
        keyName = findViewById(R.id.keyName) ?: throw Exception("Layout error")
        keyPassword = findViewById(R.id.password) ?: throw Exception("Layout error")
        keyPasswordConfirmation = findViewById(R.id.passwordConfirmation) ?: throw Exception("Layout error")
        buttonSaveKey = findViewById(R.id.buttonSaveKey) ?: throw Exception("Layout error")
        buttonCancel = findViewById(R.id.buttonCancel) ?: throw Exception("Layout error")
        textError = findViewById(R.id.textError) ?: throw Exception("Layout error")

        val keys = KeysDatabase(context = this).use { it.keys }
        for (key in keys) {

            val childLayout = layoutInflater.inflate(R.layout.key_list_item, null)

            val title = childLayout.findViewById<TextView>(R.id.textViewKeyName)
            val manageBtn = childLayout.findViewById<Button>(R.id.buttonManageKey)
            title.setText(key.name)
            manageBtn.setOnClickListener {
                if (AndroidKeyStore.isSupported)
                    AndroidKeyStore().dropKey(key.id) // this will render DB key useless - so do it first thing
                KeysDatabase(context = this).use {
                    db -> db.deleteKey(key.id)
                } // fixme -- just a debug, need to prompt what to do

                reload()
            }

            keysRoot.addView(childLayout)
        }

        addNewKeyButton.setOnClickListener {
            addNewKeyButton.visibility = View.GONE
            addKeyLayout.visibility = View.VISIBLE

        }

        buttonSaveKey.setOnClickListener {

            val name = keyName.text.toString()
            val password = keyPassword.text.toString()
            val passwordConfirm = keyPasswordConfirmation.text.toString()

            if (name.isEmpty()) {
                textError.setText("Name is empty!")
                textError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (password != passwordConfirm) {
                textError.setText("Passwords didn't match!")
                textError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val key = DerivedKeyGenerator.generate(password, "", 0, AESBinaryMessage.KEY_LEN_MAX)
                    ?: throw CryptoException("Failed to derive key")

            KeysDatabase(context = this).use {
                db ->

                val id = db.add(KeyEntry.forName("_")) // temp name to make sure it was updated

                val updatedKeyEntry =
                    if (AndroidKeyStore.isSupported) {
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

            reload()

//            addNewKeyButton.visibility = View.VISIBLE
//            addKeyLayout.visibility = View.GONE
        }

        buttonCancel.setOnClickListener {
            addNewKeyButton.visibility = View.VISIBLE
            addKeyLayout.visibility = View.GONE
        }
    }

    fun reload() {
        startActivity(Intent(this, KeysActivity::class.java))
        finish()
    }
}
