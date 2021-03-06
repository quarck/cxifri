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
import android.content.DialogInterface
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
import net.cxifri.crypto.CryptoFactory
import net.cxifri.crypto.KeyEntry
import net.cxifri.crypto.KeyRevokeMessage
import net.cxifri.keysdb.KeyHelper
import net.cxifri.keysdb.KeysDatabase
import net.cxifri.keysdb.toStringDetails
import net.cxifri.utils.PasswordComplexityEstimator
import net.cxifri.utils.UIItem
import net.cxifri.utils.background

class KeyStateEntry(
        val context: Context,
        val inflater: LayoutInflater,
        val key: KeyEntry,
        val onRevokeKey: (KeyEntry) -> Unit,
        val onDeleteKey: (KeyEntry) -> Unit,
        val onShareRevocationMessage: (KeyEntry) -> Unit
) {
    val layout: LinearLayout
    private val keyName: TextView
    private val keyDetails: TextView
    private val buttonRevoke: Button
    private val buttonDelete: Button

    init {
        layout = inflater.inflate(R.layout.key_list_item, null) as LinearLayout? ?: throw Exception("Layout error")

        keyName = layout.findViewById<TextView>(R.id.textViewKeyName) ?: throw Exception("Layout error")
        keyDetails = layout.findViewById<TextView>(R.id.textViewKeyDetails) ?: throw Exception("Layout error")
        buttonRevoke = layout.findViewById(R.id.buttonRevoke)
        buttonDelete = layout.findViewById(R.id.buttonDelete)

        buttonRevoke.setOnClickListener(this::onButtonRevoke)
        buttonDelete.setOnClickListener(this::onButtonDelete)

        keyName.text = key.name
        keyDetails.text = key.toStringDetails(context)

        if (key.revoked) {
            setRevoked()
        }
    }

    private fun onButtonRevoke(v: View) {

        AlertDialog.Builder(context)
                .setMessage(context.getString(R.string.revoke_key_question).format(key.name))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) {
                    _, _ ->
                    onRevokeKey(key)
                }
                .setNegativeButton(android.R.string.cancel) {
                    _, _ ->
                }
                .create()
                .show()
    }

    private fun onButtonDelete(v: View) {

        AlertDialog.Builder(context)
                .setMessage(context.getString(R.string.delete_key_question).format(key.name))
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

    fun setRevoked(sendMsg: Boolean = false) {
        buttonRevoke.visibility = View.GONE // no longer needed
        keyDetails.visibility = View.GONE
        layout.findViewById<TextView>(R.id.textViewKeyIsRevoked)?.visibility = View.VISIBLE
        val btnRevoke = layout.findViewById<Button>(R.id.buttonShareKeyRevokeMessage)
        btnRevoke?.visibility = View.VISIBLE
        btnRevoke?.setOnClickListener {
            onShareRevocationMessage(key)
        }

        if (sendMsg) {
            onShareRevocationMessage(key)
        }
    }
}

class KeysActivity : AppCompatActivity() {
    private val keysRoot by UIItem<LinearLayout>(R.id.layoutExistingKeysRoot)

    private val addNewPasswordKeyButton by UIItem<Button>(R.id.buttonAddNewPasswordKey)
    private val genNewKeyButton by UIItem<Button>(R.id.buttonGenerateRandomKey)
    private val scanNewKeyButton by UIItem<Button>(R.id.buttonScanRandomKey)
    private val cancelNewKeyCreationButton by UIItem<Button>(R.id.buttonCancelCreatingNewKey)

    private val viewKeysLayout by UIItem<ScrollView>(R.id.scrollViewKeys)
    private val addKeyOptionsLayout by UIItem<ScrollView>(R.id.layoutAddNewKeyButtons)
    private val addPasswordKeyLayout by UIItem<ScrollView>(R.id.layoutAddKey)

    private val keyName by UIItem<EditText>(R.id.keyName)
    private val keyPassword by UIItem<EditText>(R.id.password)
    private val keyPasswordConfirmation by UIItem<EditText>(R.id.passwordConfirmation)

    private val passwordKeyButtonSaveKey by UIItem<Button>(R.id.buttonSaveKey)
    private val passwordKeyButtonCancel by UIItem<Button>(R.id.buttonCancel)
    private val passwordKeyErrorText by UIItem<TextView>(R.id.textError)
    private val passwordKeyCBPreferAndroidKeyStore by UIItem<CheckBox>(R.id.checkBoxPreferAndroidKeyStore)

    private val pleaseWaitText by UIItem<TextView>(R.id.textDerivingKeysStatus)

    private lateinit var keyStates: MutableList<KeyStateEntry>

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
        viewKeysLayout.visibility = View.VISIBLE
        addKeyOptionsLayout.visibility = View.GONE
        addPasswordKeyLayout.visibility = View.GONE
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

    private fun doSave(name: String, password: String) {

        pleaseWaitText.visibility = View.VISIBLE

        val preferAks = passwordKeyCBPreferAndroidKeyStore.isChecked

        background {
            val key = CryptoFactory.deriveKeyFromPassword(password, name)
            KeyHelper().saveKey(this@KeysActivity, key, preferAks)

            runOnUiThread{
                pleaseWaitText.visibility = View.GONE

                viewKeysLayout.visibility = View.VISIBLE
                addKeyOptionsLayout.visibility = View.GONE
                addPasswordKeyLayout.visibility = View.GONE
                keyName.setText("")
                keyPassword.setText("")
                keyPasswordConfirmation.setText("")

                Toast.makeText(this, R.string.key_saved, Toast.LENGTH_LONG).show()
                reloadKeys()
            }
        }
    }

    private fun onButtonAddPasswordKeySave(v: View) {

        val name = keyName.text.toString()
        val password = keyPassword.text.toString()
        val passwordConfirm = keyPasswordConfirmation.text.toString()

        if (name.isEmpty()) {
            Toast.makeText(this, R.string.key_name_is_empty, Toast.LENGTH_LONG).show()
            return
        }
        else if (password != passwordConfirm) {
            Toast.makeText(this, R.string.passwords_didnt_match, Toast.LENGTH_LONG).show()
            return
        }
        else if (password.isEmpty()) {
            Toast.makeText(this, R.string.password_is_empty, Toast.LENGTH_LONG).show()
            return
        }

        val complexity  = PasswordComplexityEstimator.getExhaustiveSearchComplexity(password)
        if (!complexity.isSecure) {

            val builder = AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_launcher_foreground)
                    .setTitle(getString(R.string.password_stength_notice))
                    .setMessage(getString(R.string.still_proceed_question).format(
                            complexity.formatPc(this),
                            complexity.formatGpu(this),
                            complexity.formatCluster(this)
                    ))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        doSave(name, password)
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }

            builder.create().show()
            return
        }

        doSave(name, password)
    }


    private fun onButtonAddPasswordKeyCancel(v: View) {
        viewKeysLayout.visibility = View.VISIBLE
        addKeyOptionsLayout.visibility = View.GONE
        addPasswordKeyLayout.visibility = View.GONE
        keyName.setText("")
        keyPassword.setText("")
        keyPasswordConfirmation.setText("")
    }

    private fun onRevokeKey(key: KeyEntry){
        KeysDatabase(context = this).use {
            db -> db.update(key.copy(revoked = true))
        }

        val matchingState = keyStates.find{ it.key.key == key.key }
        if (matchingState != null) {
            matchingState.setRevoked(true)
        }
    }

    private fun onShareRevocationMessage(key: KeyEntry) {
        val handler = CryptoFactory.createMessageHandler()
        val text = handler.encrypt(KeyRevokeMessage(key), key)

        val intent = Intent(android.content.Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(android.content.Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(intent, getString(R.string.share_using)))
    }

    private fun onDeleteKey(key: KeyEntry){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // this will render DB key useless - so do it first thing
            AndroidKeyStore().dropKey(key.id)
        }

        KeysDatabase(context = this).use {
            db -> db.deleteKey(key.id)
        }

        val matchingState = keyStates.find{ it.key.key == key.key }
        if (matchingState != null) {
            keysRoot.removeView(matchingState.layout)
            keyStates.removeAll { it.key.key == key.key }
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
                    onRevokeKey = this::onRevokeKey,
                    onDeleteKey = this::onDeleteKey,
                    onShareRevocationMessage = this::onShareRevocationMessage
            )

            keyStates.add(keyState)
            keysRoot.addView(keyState.layout)
        }
    }
}
