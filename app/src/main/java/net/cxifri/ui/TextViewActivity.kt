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

package net.cxifri.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import net.cxifri.R

class TextViewActivity : AppCompatActivity() {

    lateinit var textViewAuthStatus: TextView
    lateinit var textViewMessage: TextView
    lateinit var buttonReply: Button
    lateinit var buttonQuote: Button
    lateinit var buttonCopy: Button

    var currentKeyId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_view)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayShowHomeEnabled(true)

        textViewAuthStatus = findViewById(R.id.textViewAuthStatus)
        textViewMessage = findViewById(R.id.textViewMessage)
        buttonReply = findViewById(R.id.buttonReply)
        buttonQuote = findViewById(R.id.buttonQuote)
        buttonCopy = findViewById(R.id.buttonCopy)

        buttonCopy.setOnClickListener(this::onButtonCopy)
        buttonReply.setOnClickListener(this::onButtonReply)
        buttonQuote.setOnClickListener(this::onButtonQuote)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    fun handleIntent(intent: Intent) {

        val text = intent.getStringExtra(INTENT_EXTRA_TEXT) ?: throw Exception("Must give text")
        currentKeyId = intent.getIntExtra(INTENT_EXTRA_KEY_ID, -1)
        val keyName = intent.getStringExtra(INTENT_EXTRA_KEY_NAME) ?: throw Exception("Must give key name")

        textViewMessage.setText(text)
        textViewAuthStatus.setText("Decrypted and valid, key: ${keyName}")
    }

    private fun onButtonCopy(v: View) {
        val msg = textViewMessage.text.toString()
        if (msg.length != 0) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(getString(R.string.clipboard_clip_label), msg)
            clipboard.setPrimaryClip(clip)
        }
    }

    private fun onButtonReply(v: View) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(INTENT_EXTRA_KEY_ID, currentKeyId)
        startActivity(intent)
        finish()
    }

    private fun onButtonQuote(v: View) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(INTENT_EXTRA_KEY_ID, currentKeyId)
        val text =
                textViewMessage.text.toString()
                        .split("(\\n|\\r\\n)".toRegex())
                        .map { "> $it" }
                        .joinToString("\r\n")

        intent.putExtra(INTENT_EXTRA_TEXT, text)
        startActivity(intent)
        finish()
    }

    companion object {
        const val INTENT_EXTRA_TEXT = "text"
        const val INTENT_EXTRA_KEY_ID = "keyId"
        const val INTENT_EXTRA_KEY_NAME = "keyName"
    }
}

