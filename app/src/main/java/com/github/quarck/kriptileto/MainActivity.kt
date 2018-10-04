package com.github.quarck.kriptileto

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {

    lateinit var text: TextView
    lateinit var buttonEncrypt: Button
    lateinit var buttonDecrypt: Button
    lateinit var buttonTest: Button
    lateinit var message: EditText
    lateinit var password: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        text = findViewById(R.id.textTestResult)
        buttonEncrypt = findViewById(R.id.buttonE)
        buttonDecrypt = findViewById(R.id.buttonD)
        buttonTest = findViewById(R.id.buttonT)

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

        buttonTest.setOnClickListener {
            val res = runTests()
            textTestResult.setText(res)
        }
    }

    fun runTestsForDataLen(len: Int, lg: StringBuilder) {

        val key = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)

        val dataIn = ByteArray(len)
        for (i in 0 until len) {
            dataIn[i] = i.toByte()
        }

        lg.append("Len: ${dataIn.size}; ")

        val encrypted = AESBinaryMessage.encrypt(dataIn, key)

        lg.append("encLen: ${encrypted.size}; ")

        val decrypted = AESBinaryMessage.decrypt(encrypted, key)

        if (decrypted != null) {
            var allMatch = true
            for (i in 0 until len) {
                if (dataIn[i] != decrypted[i])
                    allMatch = false
            }
            lg.append("decLen=${decrypted.size}; AllMatch: $allMatch; ")

            if (!allMatch) {
                lg.append("\n\nDECRYPT FAILED\n\n")

            }
        }
        else {
            lg.append("\n\nDECRYPT FAILED \n\n")
        }

        key[0] = 100

        val decrypted2 = AESBinaryMessage.decrypt(encrypted, key)
        if (decrypted2 == null)
            lg.append("M_OK; \n")
        else
            lg.append("MAC check failed; \n")
    }

    fun runTests(): String {
        val ret = StringBuilder()

        for (len in 1 until 666) {
            runTestsForDataLen(len, ret)
        }

        return ret.toString()
    }
}
