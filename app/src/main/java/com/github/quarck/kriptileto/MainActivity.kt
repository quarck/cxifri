package com.github.quarck.kriptileto

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import org.bouncycastle.crypto.CryptoException
import org.bouncycastle.crypto.InvalidCipherTextException
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.macs.CBCBlockCipherMac
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.bouncycastle.util.encoders.Hex
import java.io.*
import java.security.SecureRandom

class AESBinaryMessage(){
    private var cipher: PaddedBufferedBlockCipher = PaddedBufferedBlockCipher(CBCBlockCipher(AESEngine()))
    private var mac = CBCBlockCipherMac(AESEngine())
    private val cipherBlockSize = cipher.blockSize

    // Message layout:
    // [IV PLAIN TEXT] ENCRYPTED[ MAC, SALT, MESSAGE]
    // MAC = MAC of SALT + MESSAGE
    fun encrypt(message: ByteArray, key: ByteArray): ByteArray {

        val iv = ByteArray(cipherBlockSize)
        val random = SecureRandom()
        random.nextBytes(iv)

        val salt = ByteArray(cipherBlockSize)
        random.nextBytes(salt)

        val params = ParametersWithIV(KeyParameter(key), iv)
        cipher.init(true, params)

        val outputSize = iv.size + cipher.getOutputSize(mac.macSize + salt.size +  message.size) // IV goes un-encrypted

        val output = ByteArray(outputSize)

        // Copy the IV into the output
        System.arraycopy(iv, 0, output, 0, iv.size)

        var wPos = iv.size
        var remSize = outputSize - iv.size

        mac.init(KeyParameter(key))
        mac.update(salt, 0, salt.size)
        mac.update(message, 0, message.size)

        val macResult = ByteArray(mac.macSize)
        mac.doFinal(macResult, 0)

        var outL = cipher.processBytes(macResult, 0, macResult.size, output, wPos)
        wPos += outL
        remSize -= outL

        outL = cipher.processBytes(salt, 0, salt.size, output, wPos)
        wPos += outL
        remSize -= outL

        outL = cipher.processBytes(message, 0, message.size, output, wPos)
        wPos += outL
        remSize -= outL

        outL = cipher.doFinal(output, wPos)
        wPos += outL
        remSize -= outL

        if (remSize < 0)
            throw Exception("Internal error")

        if (wPos != output.size) {
            val finalOutput = ByteArray(wPos)
            System.arraycopy(output, 0, finalOutput, 0, wPos)
            return finalOutput
        }

        return output
    }

    // Returns null if decryption fails or MAC check fails
    fun decrypt(message: ByteArray, key: ByteArray): ByteArray? {

        try {
            val iv = ByteArray(cipherBlockSize)
            if (iv.size >= message.size)
                throw CryptoException()

            System.arraycopy(message, 0, iv, 0, iv.size)

            val params = ParametersWithIV(KeyParameter(key), iv)
            cipher.init(false, params)

            val outputSize = cipher.getOutputSize(message.size - iv.size)
            val decryptedRaw = ByteArray(outputSize)

            val outL = cipher.processBytes(message, iv.size, message.size - iv.size,
                    decryptedRaw, 0)

            val finalL = cipher.doFinal(decryptedRaw, outL)

            val decryptedRawL = outL + finalL

            mac.init(KeyParameter(key))

            val macCalculated = ByteArray(mac.macSize)
            val macMessage = ByteArray(mac.macSize)

            val salt = ByteArray(cipher.blockSize)

            if (salt.size + macMessage.size >= decryptedRawL)
                return null

            System.arraycopy(decryptedRaw, 0, macMessage, 0, macMessage.size)
            mac.update(decryptedRaw, macMessage.size, decryptedRawL - macMessage.size)
            mac.doFinal(macCalculated, 0)

            var matchedBytes = 0
            for (i in 0 until macCalculated.size) {
                matchedBytes += if (macCalculated[i] == macMessage[i]) 1 else 0
            }

            if (matchedBytes != macCalculated.size)
                return null

            val decrypted = ByteArray(decryptedRawL - salt.size - macMessage.size)
            System.arraycopy(decryptedRaw, salt.size + macMessage.size, decrypted, 0, decrypted.size)

            return decrypted
        }
        catch (ex: InvalidCipherTextException) {
            return null
        }
    }
}



class AESMessage(private val inputStream: BufferedInputStream,
                 private val outputStream: BufferedOutputStream,
                 private val key: ByteArray
) {
    private var cipher: PaddedBufferedBlockCipher = PaddedBufferedBlockCipher(CBCBlockCipher(AESEngine()))
    private var mac = CBCBlockCipherMac(AESEngine())

    fun performEncrypt() {
        // initialise the cipher with the key bytes, for encryption

        val iv = ByteArray(16)
        val random = SecureRandom()
        random.nextBytes(iv)

        val ivEncoded = Hex.encode(iv, 0, iv.size)
        outputStream.write(ivEncoded, 0, ivEncoded.size)

        mac.init(KeyParameter(key))
        mac.update(iv, 0, iv.size) // feed the entire IV into MAC

        val params = ParametersWithIV(KeyParameter(key), iv)
        cipher.init(true, params)

        /*
         * Create some temporary byte arrays for use in
         * encryption, make them a reasonable size so that
         * we don't spend forever reading small chunks from
         * a file.
         *
         * There is no particular reason for using getBlockSize()
         * to determine the size of the input chunk.  It just
         * was a convenient number for the example.
         */
        // int inBlockSize = cipher.getBlockSize() * 5;
        val inBlockSize = 47
        val outBlockSize = cipher.getOutputSize(inBlockSize)

        val inblock = ByteArray(inBlockSize)
        val outblock = ByteArray(outBlockSize)

        /*
         * now, read the file, and output the chunks
         */
        try {
            var inL: Int
            var outL: Int
            var rv: ByteArray? = null
            while (true) {
                inL = inputStream.read(inblock, 0, inBlockSize)
                if (inL <= 0)
                    break

                mac.update(inblock, 0, inL)

                outL = cipher.processBytes(inblock, 0, inL, outblock, 0)
                /*
                 * Before we write anything out, we need to make sure
                 * that we've got something to write outputStream.
                 */
                if (outL > 0) {
                    rv = Hex.encode(outblock, 0, outL)
                    outputStream.write(rv, 0, rv.size)
                }
            }

            // Finalize MAC and write it into the output stream

            try {
                /*
                 * Now, process the bytes that are still buffered
                 * within the cipher.
                 */
                outL = cipher.doFinal(outblock, 0)
                if (outL > 0) {
                    rv = Hex.encode(outblock, 0, outL)
                    outputStream.write(rv, 0, rv.size)
                }
            } catch (ce: CryptoException) {

            }

        } catch (ioeread: IOException) {
            ioeread.printStackTrace()
        }

    }

    fun performDecrypt() {

        val br = BufferedReader(InputStreamReader(inputStream))

        // initialise the cipher for decryption
        val ivEncoded = CharArray(16 * 2)
        val numRead = br.read(ivEncoded, 0, ivEncoded.size)
        if (numRead != 32)
            return
        val iv = Hex.decode(String(ivEncoded))
        if (iv.size != 16)
            return

        mac.init(KeyParameter(key))
        mac.update(iv, 0, iv.size) // feed the entire IV into MAC

        val params = ParametersWithIV(KeyParameter(key), iv)

        cipher.init(false, params)

        /*
         * now, read the file, and output the chunks
         */
        try {
            var outL: Int
            var inblock: ByteArray? = null
            var outblock: ByteArray? = null
            var rv: String? = null

            while (true) {
                rv = br.readLine()
                if (rv == null)
                        break

                inblock = Hex.decode(rv)
                outblock = ByteArray(cipher.getOutputSize(inblock.size))

                outL = cipher.processBytes(inblock, 0, inblock.size,
                        outblock, 0)
                /*
                 * Before we write anything out, we need to make sure
                 * that we've got something to write out.
                 */
                if (outL > 0) {
                    outputStream.write(outblock, 0, outL)
                }
            }

            try {
                /*
                 * Now, process the bytes that are still buffered
                 * within the cipher.
                 */
                outL = cipher.doFinal(outblock, 0)
                if (outL > 0) {
                    outputStream.write(outblock, 0, outL)
                }
            } catch (ce: CryptoException) {

            }

        } catch (ioeread: IOException) {
            ioeread.printStackTrace()
        }

    }

    companion object {

        fun run(text: String, key: String, encrypt: Boolean): String {


            val inputStream = BufferedInputStream(ByteArrayInputStream(text.toByteArray()))
            val output = ByteArrayOutputStream(text.length * 2)
            val outputStream = BufferedOutputStream(output)

            val finalKey = ByteArray(24)
            val keyIn = key.toByteArray()

            for (i in 0 until finalKey.size) {
                finalKey[i] = keyIn[i % keyIn.size]
            }

            val de = AESMessage(inputStream, outputStream, finalKey)
            if (encrypt)
                de.performEncrypt()
            else
                de.performDecrypt()

            inputStream.close()
            outputStream.flush()
            outputStream.close()
            output.flush()

            val ret = output.toString()
            output.close()

            return ret
        }
    }

}

class MainActivity : Activity() {

    lateinit var text: TextView
    lateinit var bE: Button
    lateinit var bD: Button

    val password = "helloWorldHelloWorld"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        text = findViewById(R.id.text)
        bE = findViewById(R.id.buttonE)
        bD = findViewById(R.id.buttonD)

        bE.setOnClickListener{
            val res = runTests()
            text.setText(res)
        }

        bD.setOnClickListener {
            val str = text.text.toString()
            text.setText(AESMessage.run(str, password, false))
        }
    }

    fun runTestsForDataLen(len: Int, lg: StringBuilder) {

        val e = AESBinaryMessage()
        val d = AESBinaryMessage()

        val key = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)

        val dataIn = ByteArray(len)
        for (i in 0 until len) {
            dataIn[i] = i.toByte()
        }

        lg.append("Len: ${dataIn.size}; ")

        val encrypted = e.encrypt(dataIn, key)

        lg.append("encLen: ${encrypted.size}; ")

        val decrypted = d.decrypt(encrypted, key)

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

        val decrypted2 = AESBinaryMessage().decrypt(encrypted, key)
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
