package com.github.quarck.kriptileto.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.github.quarck.kriptileto.aks.AndroidKeyStore
import com.github.quarck.kriptileto.crypto.DerivedKeyGenerator
import com.github.quarck.kriptileto.R
import com.github.quarck.kriptileto.keysdb.KeyEntry
import com.github.quarck.kriptileto.keysdb.KeysDatabase
import org.bouncycastle.util.encoders.UrlBase64

class KeyStateEntry(
        var context: Context,
        inflater: LayoutInflater,
        val key: KeyEntry,
        val onReplaceKey: (KeyEntry) -> Unit,
        val onDeleteKey: (KeyEntry) -> Unit
) {

    val layout: RelativeLayout
    val keyName: TextView
    val keyDetails: TextView
    val buttonReplace: Button
    val buttonDelete: Button

    init {
        layout = inflater.inflate(R.layout.key_list_item, null) as RelativeLayout? ?: throw Exception("Layout error")

        keyName = layout.findViewById<TextView>(R.id.textViewKeyName) ?: throw Exception("Layout error")
        keyDetails = layout.findViewById<TextView>(R.id.textViewKeyDetails) ?: throw Exception("Layout error")
        buttonReplace = layout.findViewById(R.id.buttonReplace)
        buttonDelete = layout.findViewById(R.id.buttonDelete)

        buttonReplace.setOnClickListener(this::onButtonReplace)
        buttonDelete.setOnClickListener(this::onButtonDelete)

        keyName.setText(key.name)
        keyDetails.setText(key.toStringDetails())
    }

    fun onButtonReplace(v: View) {

        AlertDialog.Builder(context)
                .setMessage("Key replacement is not implemented yet!")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) {
                    _, _ ->
                }
//                .setNegativeButton(android.R.string.cancel) {
//                    _, _ ->
//                }
                .create()
                .show()

        // onReplaceKey(key)
    }

    fun onButtonDelete(v: View) {

        AlertDialog.Builder(context)
                .setMessage("Delete key ${key.name}?")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) {
                    _, _ ->
                    onDeleteKey(key)
                }
                .setNegativeButton(android.R.string.cancel) {
                    _, _ ->
                }
                .create()
                .show()
    }
}

class KeysActivity : Activity() {
    lateinit var keysRoot: LinearLayout
    lateinit var addNewPasswordKeyButton: Button
    lateinit var genNewKeyButton: Button
    lateinit var scanNewKeyButton: Button
    lateinit var addKeyLayout: LinearLayout
    lateinit var keyName: EditText
    lateinit var keyPassword: EditText
    lateinit var keyPasswordConfirmation: EditText
    lateinit var buttonSaveKey: Button
    lateinit var buttonCancel: Button
    lateinit var textError: TextView
    lateinit var layoutAddNewKeyButtons: LinearLayout

    lateinit var keyStates: MutableList<KeyStateEntry>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keys)

        keysRoot = findViewById(R.id.layoutExistingKeysRoot) ?: throw Exception("Layout error")
        addNewPasswordKeyButton = findViewById(R.id.buttonAddNewPasswordKey) ?: throw Exception("Layout error")
        genNewKeyButton = findViewById(R.id.buttonGenerateRandomKey) ?: throw Exception("Layout error")
        scanNewKeyButton = findViewById(R.id.buttonScanRandomKey) ?: throw Exception("Layout error")
        addKeyLayout = findViewById(R.id.layoutAddKey) ?: throw Exception("Layout error")
        keyName = findViewById(R.id.keyName) ?: throw Exception("Layout error")
        keyPassword = findViewById(R.id.password) ?: throw Exception("Layout error")
        keyPasswordConfirmation = findViewById(R.id.passwordConfirmation) ?: throw Exception("Layout error")
        buttonSaveKey = findViewById(R.id.buttonSaveKey) ?: throw Exception("Layout error")
        buttonCancel = findViewById(R.id.buttonCancel) ?: throw Exception("Layout error")
        textError = findViewById(R.id.textError) ?: throw Exception("Layout error")
        layoutAddNewKeyButtons = findViewById(R.id.layoutAddNewKeyButtons) ?: throw Exception("Layout error")

        addKeyLayout.visibility = View.GONE

        val keys = KeysDatabase(context = this).use { it.keys }
        keyStates = mutableListOf<KeyStateEntry>()

        for (key in keys) {
            val keyState = KeyStateEntry(
                    context = this,
                    inflater = layoutInflater,
                    key = key,
                    onReplaceKey = this::onReplaceKey,
                    onDeleteKey = this::onDeleteKey
            )

            keyStates.add(keyState)
            keysRoot.addView(keyState.layout)

        }

        addNewPasswordKeyButton.setOnClickListener(this::onButtonAddPasswordKey)
        genNewKeyButton.setOnClickListener(this::onButtonGenerateKey)
        scanNewKeyButton.setOnClickListener(this::onButtonScanNewKey)

        buttonSaveKey.setOnClickListener(this::onButtonAddPasswordKeySave)

        buttonCancel.setOnClickListener(this::onButtonAddPasswordKeyCancel)
    }

    private fun onButtonAddPasswordKey(v: View) {
        layoutAddNewKeyButtons.visibility = View.GONE
        addKeyLayout.visibility = View.VISIBLE
    }

    private fun onButtonGenerateKey(v: View) {
        val intent = Intent(this, RandomKeyGenerationActivity::class.java)
        startActivity(intent)
    }

    private fun onButtonScanNewKey(v: View) {
        val intent = Intent(this, RandomKeyScanActivity::class.java)
        startActivity(intent)
    }

    private fun onButtonAddPasswordKeySave(v: View) {

        val name = keyName.text.toString()
        val password = keyPassword.text.toString()
        val passwordConfirm = keyPasswordConfirmation.text.toString()

        val onError = {
            text: String ->
            textError.setText(text)
            textError.visibility = View.VISIBLE
        }

        if (name.isEmpty()) {
            onError("Name is empty!")
            return
        }
        else if (password != passwordConfirm) {
            onError("Passwords didn't match!")
            return
        }
        else if (password.isEmpty()) {
            onError("Password is empty")
            return
        }
        else if (password.length < 8) {
            onError("Password is way too short - 8 chars min")
            return
        }

        val key = DerivedKeyGenerator().generateForAESTwofishSerpent(password)

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

//            addNewPasswordKeyButton.visibility = View.VISIBLE
//            addKeyLayout.visibility = View.GONE
    }


    private fun onButtonAddPasswordKeyCancel(v: View) {
        layoutAddNewKeyButtons.visibility = View.VISIBLE
        addKeyLayout.visibility = View.GONE
    }

    private fun onReplaceKey(key: KeyEntry){
        // Ignored - we don't have it implemented yet
    }

    private fun onDeleteKey(key: KeyEntry){

        if (AndroidKeyStore.isSupported) {
            // this will render DB key useless - so do it first thing
            AndroidKeyStore().dropKey(key.id)
        }

        KeysDatabase(context = this).use {
            db -> db.deleteKey(key.id)
        }

        reload()
    }

    fun reload() {
        startActivity(Intent(this, KeysActivity::class.java))
        finish()
    }
}
