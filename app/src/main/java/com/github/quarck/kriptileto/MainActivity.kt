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
import android.widget.TextView
import org.bouncycastle.crypto.CryptoException
import org.bouncycastle.crypto.engines.AESEngine


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
    lateinit var textViewError: TextView

    var isTextEncrypted = false

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
        textViewError = findViewById(R.id.textViewError)

        message = findViewById(R.id.message)
        password = findViewById(R.id.password)

        buttonKeySelect.setOnClickListener(this::onButtonKeySelect)
        buttonEncrypt.setOnClickListener(this::onButtonEncrypt)
        buttonDecrypt.setOnClickListener(this::onButtonDecrypt)

        buttonShare.setOnClickListener(this::onButtonShare)

        buttonCopy.setOnClickListener(this::onButtonCopy)
        buttonPaste.setOnClickListener(this::onButtonPaste)

        buttonClear.setOnClickListener(this::onButtonClear)

        buttonManageKeys.setOnClickListener(this::onButtonManageKeys)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    fun handleIntent(intent: Intent) {
        val intentTextExtra = intent.getStringExtra(INTENT_EXTRA_TEXT)
        val intentKeyIdExtra = intent.getLongExtra(INTENT_EXTRA_KEY_ID, -1L)

        if (intentKeyIdExtra != -1L) {
            onKeySelected(intentKeyIdExtra)
            if (intentTextExtra != null) {
                message.setText(intentTextExtra)
                isTextEncrypted = false
                buttonShare.isEnabled = false
            }
        }
        else if (intentTextExtra != null) {
            message.setText(intentTextExtra)
            isTextEncrypted = false
            buttonShare.isEnabled = false
        }
        else if (intent.action == Intent.ACTION_SEND) {
            val text = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
            if (text != null) {
                message.setText(text)
                isTextEncrypted = false
                buttonShare.isEnabled = false
            }
        }
        else {
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
                val keyId = values[which]
                onKeySelected(keyId)
            }
        }

        return builder.create().show()

    }

    private fun onKeySelected(keyId: Long) {
        if (keyId != -1L) {
            password.visibility = View.GONE
            currentKey = KeysDatabase(context = this).use { it.getKey(keyId) }
            buttonKeySelect.text = "key: ${currentKey?.name}"
        }
        else {
            password.visibility = View.VISIBLE
            buttonKeySelect.text = "key: type below"
            currentKey = null
        }
    }



    private fun onButtonEncrypt(v: View) {
        val msg = message.text.toString()
        val key = password.text.toString()

        textViewError.visibility = View.VISIBLE

        background {
            var encrypted: String? = null
            val cKey = currentKey

            if (cKey != null) {
                try {
                    encrypted = KriptiletoMessage().encrypt(msg, cKey)
                }
                catch (ex: Exception){
                    encrypted = null
                }
            }
            else {
                if (key.isEmpty()) {
                    runOnUiThread {
                        textViewError.setText("Key is empty")
                        textViewError.visibility = View.VISIBLE
                    }
                }
                else if (key.length < 8) {
                    runOnUiThread {
                        textViewError.setText("Key is too short (min 8 chars)")
                        textViewError.visibility = View.VISIBLE
                    }
                }
                else {
                    encrypted = KriptiletoMessage().encrypt(msg, key)
                }
            }

            if (encrypted != null) {
                isTextEncrypted = true
                runOnUiThread{
                    message.setText(encrypted)
                    buttonShare.isEnabled = isTextEncrypted
                }
            }
        }
    }

    private fun onButtonDecrypt(v: View) {
        val msg = message.text.toString()
        val key = password.text.toString()

        textViewError.visibility = View.VISIBLE

        background {
            try {
                val cKey = currentKey
                val decrypted =
                        if (cKey != null)
                            KriptiletoMessage().decrypt(msg, cKey)
                        else
                            KriptiletoMessage().decrypt(msg, key)

                if (decrypted != null) {
                    isTextEncrypted = false
                    runOnUiThread{
                        message.setText(decrypted)
                        buttonShare.isEnabled = isTextEncrypted
                    }
                }
            } catch (ex: Exception) {
            }
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

    companion object {
        const val INTENT_EXTRA_TEXT = "text"
        const val INTENT_EXTRA_KEY_ID = "keyId"
    }
}

