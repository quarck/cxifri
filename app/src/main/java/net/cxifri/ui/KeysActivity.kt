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

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import net.cxifri.aks.AndroidKeyStore
import net.cxifri.crypto.DerivedKeyGenerator
import net.cxifri.R
import net.cxifri.crypto.KeyEntry
import net.cxifri.keysdb.KeyHelper
import net.cxifri.keysdb.KeysDatabase
import net.cxifri.keysdb.toStringDetails
import net.cxifri.utils.UIItem

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
        keyDetails.setText(key.toStringDetails(context))
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

class KeysActivity : AppCompatActivity() {
    val keysRoot by UIItem<LinearLayout>(R.id.layoutExistingKeysRoot)

    val addNewPasswordKeyButton by UIItem<Button>(R.id.buttonAddNewPasswordKey)
    val genNewKeyButton by UIItem<Button>(R.id.buttonGenerateRandomKey)
    val scanNewKeyButton by UIItem<Button>(R.id.buttonScanRandomKey)
    val cancelNewKeyCreationButton by UIItem<Button>(R.id.buttonCancelCreatingNewKey)

    val viewKeysLayout by UIItem<ScrollView>(R.id.scrollViewKeys)
    val addKeyOptionsLayout by UIItem<ScrollView>(R.id.layoutAddNewKeyButtons)
    val addPasswordKeyLayout by UIItem<ScrollView>(R.id.layoutAddKey)

    val keyName by UIItem<EditText>(R.id.keyName)
    val keyPassword by UIItem<EditText>(R.id.password)
    val keyPasswordConfirmation by UIItem<EditText>(R.id.passwordConfirmation)

    val passwordKeyButtonSaveKey by UIItem<Button>(R.id.buttonSaveKey)
    val passwordKeyButtonCancel by UIItem<Button>(R.id.buttonCancel)
    val passwordKeyErrorText by UIItem<TextView>(R.id.textError)
    val passwordKeyCBPreferAndroidKeyStore by UIItem<CheckBox>(R.id.checkBoxPreferAndroidKeyStore)

//    val layoutAddNewKeyButtons by UIItem<LinearLayout>(R.id.layoutAddNewKeyButtons)

    lateinit var keyStates: MutableList<KeyStateEntry>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keys)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        addNewPasswordKeyButton.setOnClickListener(this::onButtonAddPasswordKey)
        genNewKeyButton.setOnClickListener(this::onButtonGenerateKey)
        scanNewKeyButton.setOnClickListener(this::onButtonScanNewKey)
        cancelNewKeyCreationButton.setOnClickListener(this::onButtonCancelNewKey)

        passwordKeyButtonSaveKey.setOnClickListener(this::onButtonAddPasswordKeySave)
        passwordKeyButtonCancel.setOnClickListener(this::onButtonAddPasswordKeyCancel)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_keys_activity, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.menu_add_key ->
                onAddKey()

            R.id.menu_remove_all ->
                onDeleteAllKeys()
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onResume() {
        super.onResume()
        reloadKeys()
    }

    private fun onAddKey() {
        viewKeysLayout.visibility = View.GONE
        addKeyOptionsLayout.visibility = View.VISIBLE
        addPasswordKeyLayout.visibility = View.GONE
    }

    private fun onButtonAddPasswordKey(v: View) {
        viewKeysLayout.visibility = View.GONE
        addKeyOptionsLayout.visibility = View.GONE
        addPasswordKeyLayout.visibility = View.VISIBLE
    }

    private fun onButtonGenerateKey(v: View) {
        val intent = Intent(this, RandomKeyQRCodeShareActivity::class.java)
        startActivity(intent)
    }

    private fun onButtonScanNewKey(v: View) {
        val intent = Intent(this, RandomKeyQRCodeScanActivity::class.java)
        startActivity(intent)
    }

    private fun onButtonCancelNewKey(v: View) {
        viewKeysLayout.visibility = View.VISIBLE
        addKeyOptionsLayout.visibility = View.GONE
        addPasswordKeyLayout.visibility = View.GONE
    }

    private fun onButtonAddPasswordKeySave(v: View) {

        val name = keyName.text.toString()
        val password = keyPassword.text.toString()
        val passwordConfirm = keyPasswordConfirmation.text.toString()

        val onError = {
            text: String ->
            passwordKeyErrorText.setText(text)
            passwordKeyErrorText.visibility = View.VISIBLE
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
        KeyHelper().saveKey(this, name, key, passwordKeyCBPreferAndroidKeyStore.isChecked)

        viewKeysLayout.visibility = View.VISIBLE
        addKeyOptionsLayout.visibility = View.GONE
        addPasswordKeyLayout.visibility = View.GONE
        keyName.setText("")
        keyPassword.setText("")
        keyPasswordConfirmation.setText("")

        reloadKeys()
    }


    private fun onButtonAddPasswordKeyCancel(v: View) {
        viewKeysLayout.visibility = View.VISIBLE
        addKeyOptionsLayout.visibility = View.GONE
        addPasswordKeyLayout.visibility = View.GONE
        keyName.setText("")
        keyPassword.setText("")
        keyPasswordConfirmation.setText("")
    }

    private fun onReplaceKey(key: KeyEntry){
        // Ignored - we don't have it implemented yet
    }

    private fun onDeleteKey(key: KeyEntry){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // this will render DB key useless - so do it first thing
            AndroidKeyStore().dropKey(key.id)
        }

        KeysDatabase(context = this).use {
            db -> db.deleteKey(key.id)
        }

        val matchingState = keyStates.find{ it.key.value == key.value }
        if (matchingState != null) {
            keysRoot.removeView(matchingState.layout)
            keyStates.removeAll { it.key.value == key.value }
        }
    }

    private fun onDeleteAllKeys() {
        val builder = AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.delete_all_keys_question))
                .setMessage(getString(R.string.delete_all_keys_can_be_undone))
                .setPositiveButton(android.R.string.ok) { _, _ -> deleteAllKeysSecondConfirmation() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
        return builder.create().show()
    }

    private fun deleteAllKeysSecondConfirmation() {
        val builder = AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.delete_all_keys_second_confirmation_question))
                .setPositiveButton(android.R.string.ok) { _, _ -> doDeleteAllKeys() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
        return builder.create().show()
    }

    private fun doDeleteAllKeys() {
        val keys = KeysDatabase(context = this).use { it.keys }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // this will render DB key useless - so do it first thing
            AndroidKeyStore().let {
                for (key in keys)
                    it.dropKey(key.id)
            }
        }

        KeysDatabase(context = this).use {
            db ->
            for (key in keys)
                db.deleteKey(key.id)
        }

        reloadKeys()
    }

    fun reloadKeys() {
        keysRoot.removeAllViews()

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
    }
}
