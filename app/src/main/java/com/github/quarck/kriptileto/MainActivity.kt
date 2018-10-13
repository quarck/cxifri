package com.github.quarck.kriptileto

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.ArrayAdapter


class MainActivity : Activity() {

    lateinit var buttonEncrypt: Button
    lateinit var buttonDecrypt: Button
    lateinit var buttonShare: Button
    lateinit var buttonPaste: Button
    lateinit var buttonCopy: Button
    lateinit var buttonClear: Button
    lateinit var message: EditText
    lateinit var buttonKeySelect: Button
    lateinit var password: EditText
    lateinit var buttonManageKeys: Button

    var isTextEncrypted = false

    var currentKeyId: Long = -1
    var currentKey: KeyEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonEncrypt = findViewById(R.id.buttonEncrypt)
        buttonDecrypt = findViewById(R.id.buttonDecrypt)
        buttonShare = findViewById(R.id.buttonShare)
        buttonCopy = findViewById(R.id.buttonCopy)
        buttonPaste = findViewById(R.id.buttonPaste)
        buttonClear = findViewById(R.id.buttonClear)
        buttonManageKeys = findViewById(R.id.buttonManageKeys)
        buttonKeySelect = findViewById(R.id.buttonKeySelect)

        message = findViewById(R.id.message)
        password = findViewById(R.id.password)

        handleIntent(intent)

        buttonKeySelect.setOnClickListener(this::onButtonKeySelect)
        buttonEncrypt.setOnClickListener(this::onButtonEncrypt)
        buttonDecrypt.setOnClickListener(this::onButtonDecrypt)

        buttonShare.setOnClickListener(this::onButtonShare)

        buttonCopy.setOnClickListener(this::onButtonCopy)
        buttonPaste.setOnClickListener(this::onButtonPaste)

        buttonClear.setOnClickListener(this::onButtonClear)

        buttonManageKeys.setOnClickListener(this::onButtonManageKeys)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    fun handleEncryptedTextIntent(text: String) {

        val keys = KeysDatabase(context = this).use { it.keys }

        val unwrapped = UrlWrapper.unwrap(text) ?: text

        var success = false

        for (key in keys) {
            val decrypted = AESTextMessage.decrypt(unwrapped, key)
            if (decrypted != null) {
                message.setText(decrypted)
                isTextEncrypted = false
                buttonShare.isEnabled = isTextEncrypted

                password.visibility = View.GONE
                buttonKeySelect.text = "key: ${key.name}"
                currentKey = key

                success = true
            }
        }

        if (!success) {
            message.setText(text)  // original encrypted
            isTextEncrypted = true
            buttonShare.isEnabled = isTextEncrypted

            password.visibility = View.VISIBLE
            buttonKeySelect.text = "key: type below"
            currentKey = null
        }
    }

    fun handleSharedRawTextIntent(text: String) {
        message.setText(text)
        isTextEncrypted = false
        buttonShare.isEnabled = isTextEncrypted
    }

    fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                if (uri != null)
                    handleEncryptedTextIntent(uri.toString())
            }
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
                if (text != null)
                    handleSharedRawTextIntent(text)
            }
            else ->
                buttonShare.isEnabled = false
        }
    }

    private fun onButtonKeySelect(v: View) {
        val keys = KeysDatabase(context = this).use { it.keys }

        val builder = AlertDialog.Builder(this)
        builder.setIcon(R.drawable.ic_launcher_foreground)
        builder.setTitle("Pick a key")

        val names = listOf<String>("Custom") + keys.map { it.name }.toList()
        val values = listOf<Long>(-1L) + keys.map { it.id }.toList()

        val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice, names)

        builder.setNegativeButton(android.R.string.cancel) {
            dialog, which -> dialog.dismiss()
        }

        builder.setAdapter(arrayAdapter) {
            dialog, which ->
            if (which >= 0 && which < values.size && which < names.size) {
                val name = names[which]
                val currentKeyId = values[which]

                onKeySelected(name, currentKeyId)
            }
        }

        return builder.create().show()

    }

    private fun onKeySelected(name: String, keyId: Long) {
        if (keyId != -1L) {
            password.visibility = View.GONE
            buttonKeySelect.text = "key: $name"
            currentKey = KeysDatabase(context = this).use { it.getKey(keyId) }
        }
        else {
            password.visibility = View.VISIBLE
            buttonKeySelect.text = "key: type below"
            currentKey = null
        }
    }

    private fun onButtonEncrypt(v: View) {
        val msg = message.text.toString()

        val encrypted =
                if (currentKey != null)
                    AESTextMessage.encrypt(msg, currentKey!!) // FIXME
                else
                    AESTextMessage.encrypt(msg, password.text.toString())

        message.setText(UrlWrapper.wrap(encrypted))
        isTextEncrypted = true
        buttonShare.isEnabled = isTextEncrypted
    }

    private fun onButtonDecrypt(v: View) {
        val msg = message.text.toString()

        val unwrapped = UrlWrapper.unwrap(msg)

        val decrypted =
                if (currentKey != null)
                    AESTextMessage.decrypt(unwrapped ?: msg, currentKey!!) // FIXME
                else
                    AESTextMessage.decrypt(unwrapped ?: msg, password.text.toString())

        if (decrypted != null) {
            message.setText(decrypted)
            isTextEncrypted = false
            buttonShare.isEnabled = isTextEncrypted
        }
    }

    private fun onButtonCopy(v: View) {
        val msg = message.text.toString()
        if (msg.length != 0) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(getString(R.string.clipboard_clip_label), msg)
            clipboard.setPrimaryClip(clip)
        }
    }

    private fun onButtonPaste(v: View) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.getPrimaryClip()
        if (clip != null) {
            val item = clip.getItemAt(0)
            if (item != null)
                message.setText(item.text)
        }

        isTextEncrypted = false
        buttonShare.isEnabled = isTextEncrypted
    }

    private fun onButtonClear(v: View) {
        message.setText("")
        password.setText("")
        isTextEncrypted = false
        buttonShare.isEnabled = isTextEncrypted
    }

    private fun onButtonShare(v: View) {
        if (isTextEncrypted) {
            val intent = Intent(android.content.Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(android.content.Intent.EXTRA_TEXT, message.text.toString())
            startActivity(Intent.createChooser(intent, getString(R.string.share_using)))
        }
    }

    private fun onButtonManageKeys(v: View) {
        val intent = Intent(this, KeysActivity::class.java)
        startActivity(intent)
    }
}

