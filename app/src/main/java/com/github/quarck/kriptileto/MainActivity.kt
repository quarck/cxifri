package com.github.quarck.kriptileto

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent




class MainActivity : Activity() {

    lateinit var buttonEncrypt: Button
    lateinit var buttonDecrypt: Button
    lateinit var buttonShare: Button
    lateinit var buttonPaste: Button
    lateinit var buttonCopy: Button
    lateinit var buttonClear: Button
    lateinit var message: EditText
    lateinit var password: EditText

    var isTextEncrypted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonEncrypt = findViewById(R.id.buttonEncrypt)
        buttonDecrypt = findViewById(R.id.buttonDecrypt)
        buttonShare = findViewById(R.id.buttonShare)
        buttonCopy = findViewById(R.id.buttonCopy)
        buttonPaste = findViewById(R.id.buttonPaste)
        buttonClear = findViewById(R.id.buttonClear)

        message = findViewById(R.id.message)
        password = findViewById(R.id.password)

        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                if (uri != null) {
                    message.setText(uri.toString())
                    isTextEncrypted = true
                    buttonShare.isEnabled = isTextEncrypted
                }
            }
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
                if (text != null) {
                    message.setText(text)
                    isTextEncrypted = false
                    buttonShare.isEnabled = isTextEncrypted
                }
            }
        }

        buttonEncrypt.setOnClickListener{
            val msg = message.text.toString()
            val pass = password.text.toString()
            val encrypted = AESTextMessage.encrypt(msg, pass)
            message.setText(UrlWrapper.wrap(encrypted))
            isTextEncrypted = true
            buttonShare.isEnabled = isTextEncrypted
        }

        buttonShare.setOnClickListener {
            if (isTextEncrypted) {
                val intent = Intent(android.content.Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(android.content.Intent.EXTRA_TEXT, message.text.toString())
                startActivity(Intent.createChooser(intent, getString(R.string.share_using)))
            }
        }

        buttonDecrypt.setOnClickListener {
            val msg = message.text.toString()
            val pass = password.text.toString()

            val unwrapped = UrlWrapper.unwrap(msg)
            val decrypted = AESTextMessage.decrypt(unwrapped ?: msg, pass)
            if (decrypted != null) {
                message.setText(decrypted)
                isTextEncrypted = false
                buttonShare.isEnabled = isTextEncrypted
            }
        }

        buttonCopy.setOnClickListener {
            val msg = message.text.toString()
            if (msg.length != 0) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(getString(R.string.clipboard_clip_label), msg)
                clipboard.setPrimaryClip(clip)
            }
        }

        buttonPaste.setOnClickListener {
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

        buttonClear.setOnClickListener {
            message.setText("")
            password.setText("")
            isTextEncrypted = false
            buttonShare.isEnabled = isTextEncrypted
        }
    }
}
