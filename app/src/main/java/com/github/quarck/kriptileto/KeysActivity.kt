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

class KeysActivity : Activity() {
    lateinit var keysRoot: LinearLayout
    lateinit var addNewKeyButton: Button
    lateinit var addKeyLayout: LinearLayout
    lateinit var keyPassword: EditText
    lateinit var buttonSaveKey: Button
    lateinit var buttonCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keys)

        keysRoot = findViewById(R.id.layoutExistingKeysRoot) ?: throw Exception("Layout error")
        addNewKeyButton = findViewById(R.id.buttonAddNewKey) ?: throw Exception("Layout error")
        addKeyLayout = findViewById(R.id.layoutAddKey) ?: throw Exception("Layout error")
        keyPassword = findViewById(R.id.password) ?: throw Exception("Layout error")
        buttonSaveKey = findViewById(R.id.buttonSaveKey) ?: throw Exception("Layout error")
        buttonCancel = findViewById(R.id.buttonCancel) ?: throw Exception("Layout error")

        val keys = KeysDatabase(context = this).use { it.keys }
        for (key in keys) {

            val childLayout = layoutInflater.inflate(R.layout.key_list_item, null)

            val title = childLayout.findViewById<TextView>(R.id.textViewKeyName)
            val manageBtn = childLayout.findViewById<Button>(R.id.buttonManageKey)
            title.setText(key.name)
            manageBtn.setOnClickListener {
                KeysDatabase(context = this).use { db -> db.deleteKey(key.id) } // fixme -- just a debug, need to prompt what to do
            }

            keysRoot.addView(childLayout)
        }

        addNewKeyButton.setOnClickListener {
            addNewKeyButton.visibility = View.GONE
            addKeyLayout.visibility = View.VISIBLE

        }

        buttonSaveKey.setOnClickListener {

            val password = keyPassword.text.toString()

            val key = DerivedKeyGenerator.generate(password, "", 0, AESBinaryMessage.KEY_LEN_MAX)
                    ?: throw CryptoException("Failed to derive key")

            val name = System.currentTimeMillis().toString() // FIXME
            KeysDatabase(context = this).use { db -> db.add(KeyEntry.fromBinaryPTKey(name, key)) }

            addNewKeyButton.visibility = View.VISIBLE
            addKeyLayout.visibility = View.GONE
        }

        buttonCancel.setOnClickListener {
            addNewKeyButton.visibility = View.VISIBLE
            addKeyLayout.visibility = View.GONE
        }
    }
}
