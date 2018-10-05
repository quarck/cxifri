package com.github.quarck.kriptileto

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import android.R.attr.label
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE



class MainActivity : Activity() {

    lateinit var buttonEncrypt: Button
    lateinit var buttonDecrypt: Button
    lateinit var buttonPaste: Button
    lateinit var buttonCopy: Button
    lateinit var buttonClear: Button
    lateinit var message: EditText
    lateinit var password: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonEncrypt = findViewById(R.id.buttonEncrypt)
        buttonDecrypt = findViewById(R.id.buttonDecrypt)
        buttonCopy = findViewById(R.id.buttonCopy)
        buttonPaste = findViewById(R.id.buttonPaste)
        buttonClear = findViewById(R.id.buttonClear)

        message = findViewById(R.id.message)
        password = findViewById(R.id.password)

        buttonEncrypt.setOnClickListener{
            val msg = message.text.toString()
            val pass = password.text.toString()
            val encrypted = AESTextMessage.encrypt(msg, pass)
            message.setText(encrypted)
        }

        buttonDecrypt.setOnClickListener {
            val msg = message.text.toString()
            val pass = password.text.toString()
            val decrypted = AESTextMessage.decrypt(msg, pass)
            if (decrypted != null)
                message.setText(decrypted)
        }

        buttonCopy.setOnClickListener {
            val msg = message.text.toString()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(getString(R.string.clipboard_clip_label), msg)
            clipboard.setPrimaryClip(clip)
        }

        buttonPaste.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.getPrimaryClip()
            if (clip != null) {
                val item = clip.getItemAt(0)
                if (item != null)
                    message.setText(item.text)
            }
        }

        buttonClear.setOnClickListener {
            message.setText("")
            password.setText("")
        }
    }
}
