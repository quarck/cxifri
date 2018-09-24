package com.github.quarck.kriptileto

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import org.bouncycastle.crypto.CryptoException
import org.bouncycastle.crypto.KeyGenerationParameters
import org.bouncycastle.crypto.engines.DESedeEngine
import org.bouncycastle.crypto.generators.DESedeKeyGenerator
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.params.DESedeParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.util.encoders.Hex
import java.io.*
import java.security.SecureRandom

class DESExample(private val inputStream: BufferedInputStream,
                 private val outputStream: BufferedOutputStream,
                 private val key: ByteArray,
                 private val encrypt: Boolean
) {
    // To hold the initialised DESede cipher
    private var cipher: PaddedBufferedBlockCipher = PaddedBufferedBlockCipher(CBCBlockCipher(DESedeEngine()))

    fun performEncrypt() {
        // initialise the cipher with the key bytes, for encryption

        cipher.init(true, KeyParameter(key))

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

                outL = cipher.processBytes(inblock, 0, inL, outblock, 0)
                /*
                 * Before we write anything out, we need to make sure
                 * that we've got something to write outputStream.
                 */
                if (outL > 0) {
                    rv = Hex.encode(outblock, 0, outL)
                    outputStream.write(rv, 0, rv.size)
                    outputStream.write("\n".toByteArray())
                }
            }

            try {
                /*
                 * Now, process the bytes that are still buffered
                 * within the cipher.
                 */
                outL = cipher.doFinal(outblock, 0)
                if (outL > 0) {
                    rv = Hex.encode(outblock, 0, outL)
                    outputStream.write(rv, 0, rv.size)
                    outputStream.write("\n".toByteArray())
                }
            } catch (ce: CryptoException) {

            }

        } catch (ioeread: IOException) {
            ioeread.printStackTrace()
        }

    }

    fun performDecrypt() {

        // initialise the cipher for decryption
        cipher.init(false, KeyParameter(key))

        /*
         * As the decryption is from our preformatted file,
         * and we know that it's a hex encoded format, then
         * we wrap the InputStream with a BufferedReader
         * so that we can read it easily.
         */
        val br = BufferedReader(InputStreamReader(inputStream))

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

            val de = DESExample(inputStream, outputStream, key.toByteArray(), encrypt)
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
            val str = text.text.toString()
            text.setText(DESExample.run(str, password, true))
        }

        bD.setOnClickListener {
            val str = text.text.toString()
            text.setText(DESExample.run(str, password, false))
        }
    }
}
