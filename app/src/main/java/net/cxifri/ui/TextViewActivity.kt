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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import net.cxifri.R
import net.cxifri.utils.UIItem

class TextViewActivity : AppCompatActivity() {

    val textViewMatchedKey by UIItem<TextView>(R.id.textViewMatchedKey)
    val textViewAuthStatusValid by UIItem<TextView>(R.id.textViewAuthStatusValid)
    val textViewMessage by UIItem<TextView>(R.id.textViewMessage)
    val textViewKeyRevoked by UIItem<TextView>(R.id.textViewKeyIsRevoked)

    var currentKeyId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_view)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    fun handleIntent(intent: Intent) {
        val text = intent.getStringExtra(INTENT_EXTRA_TEXT) ?: throw Exception("Must give text")
        currentKeyId = intent.getLongExtra(INTENT_EXTRA_KEY_ID, -1L)
        val keyName = intent.getStringExtra(INTENT_EXTRA_KEY_NAME) ?: throw Exception("Must give key name")

        textViewMessage.text = text
        textViewMatchedKey.text = getString(R.string.matched_key).format(keyName)
        textViewAuthStatusValid.visibility = View.VISIBLE
        textViewKeyRevoked.visibility =
                if (intent.getBooleanExtra(INTENT_EXTRA_KEY_IS_NOT_REVOKED, false)) View.GONE else View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_view_text, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.menu_copy ->
                onMenuCopy()

            R.id.menu_quote ->
                onMenuQuote()

            R.id.menu_reply ->
                onMenuReply()
        }
        return super.onOptionsItemSelected(item)
    }



    private fun onMenuCopy() {
        val msg = textViewMessage.text.toString()
        if (msg.length != 0) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(getString(R.string.clipboard_clip_label), msg)
            clipboard.setPrimaryClip(clip)
        }

        Toast.makeText(this, R.string.text_copied, Toast.LENGTH_SHORT).show()
    }

    private fun onMenuReply() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(INTENT_EXTRA_KEY_ID, currentKeyId)
        intent.putExtra(INTENT_EXTRA_TEXT, "")
        startActivity(intent)
        finish()
    }

    private fun onMenuQuote() {
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
        const val INTENT_EXTRA_KEY_IS_NOT_REVOKED = "notRevoked"
    }
}

