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

import android.os.Bundle
import android.app.Activity
import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import net.cxifri.R
import net.cxifri.aks.AndroidKeyStore
import net.cxifri.crypto.AESTwofishSerpentEngine
import net.cxifri.crypto.RandomKeyGenerator
import net.cxifri.dataprocessing.QREncoder
import net.cxifri.keysdb.KeyEntry
import net.cxifri.keysdb.KeySaveHelper
import net.cxifri.keysdb.KeysDatabase

import org.bouncycastle.util.encoders.UrlBase64
import android.view.WindowManager
import android.content.ContentValues.TAG
import android.R.attr.y
import android.R.attr.x
import android.graphics.Point
import android.util.Log
import android.view.Display





class RandomKeyQRCodeShareActivity : Activity() {

    lateinit var key: ByteArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_random_key_generation)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        this.window.attributes = this.window.attributes.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        }

        val imageView = findViewById<ImageView>(R.id.imageViewQR)
        val gen = QREncoder(Math.max(getDimensions()*8/10, 512))

        val (keyBits, csum) = RandomKeyGenerator().generateKeywithCSum(AESTwofishSerpentEngine.KEY_LENGTH_BYTES)
        key = keyBits
        val base64key = UrlBase64.encode(key + csum).toString(charset = Charsets.UTF_8)

        val img = gen.encodeAsBitmap(base64key)

        imageView.setImageBitmap(img)

        findViewById<Button>(R.id.buttonSave).setOnClickListener(this::onButtonSave)

        val name = findViewById<EditText>(R.id.editTextKeyName)
        name.setOnFocusChangeListener { _, hasFocus ->
            this.window.attributes = this.window.attributes.apply {
                screenBrightness =
                        if (hasFocus)
                            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                        else
                            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            }
        }
    }

    private fun getDimensions(): Int {
        val display = windowManager.defaultDisplay
        return Point().apply {display.getSize(this)}.x
    }

    private fun onButtonSave(v: View) {
        val name = findViewById<EditText>(R.id.editTextKeyName)

        name?.text?.toString()?.let {
            KeySaveHelper().saveKey(this, it, key, true)
            finish()
        }
    }
}
