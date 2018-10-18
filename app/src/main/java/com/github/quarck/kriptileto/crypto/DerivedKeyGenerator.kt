package com.github.quarck.kriptileto.crypto

import com.github.quarck.kriptileto.utils.wipe
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.KeyParameter

class DerivedKeyGenerator {

    val DEFAULT_NUM_ITERATIONS = 10000
    val SALT_AES = "kriptileto-AES"
    val SALT_TWOFISH = "kriptileto-Twofish"
    val SALT_SERPENT = "kriptileto-Serpent"

    fun generateForAES(password: String): ByteArray {
        val pGen = PKCS5S2ParametersGenerator(SHA256Digest())
        pGen.init(
                password.toByteArray(charset = Charsets.UTF_8),
                SALT_AES.toByteArray(charset = Charsets.UTF_8),
                DEFAULT_NUM_ITERATIONS
        )
        val keyLenBytes = 32
        val key = pGen.generateDerivedMacParameters(keyLenBytes * 8) as KeyParameter
        return key.key
    }

    fun generateForAESTwofishSerpent(password: String): ByteArray {
        val keyLenBytesEach = 32

        val saltAES = SALT_AES.toByteArray(charset = Charsets.UTF_8)
        val saltTwofish = SALT_TWOFISH.toByteArray(charset = Charsets.UTF_8)
        val saltSerpent = SALT_SERPENT.toByteArray(charset = Charsets.UTF_8)

        val pGenAES = PKCS5S2ParametersGenerator(SHA256Digest())
        val pGenTwofish = PKCS5S2ParametersGenerator(SHA256Digest())
        val pGenSerpent = PKCS5S2ParametersGenerator(SHA256Digest())

        val passAsByteArray = password.toByteArray(charset = Charsets.UTF_8)
        pGenAES.init(passAsByteArray, saltAES, DEFAULT_NUM_ITERATIONS)
        pGenTwofish.init(passAsByteArray, saltTwofish, DEFAULT_NUM_ITERATIONS)
        pGenSerpent.init(passAsByteArray, saltSerpent, DEFAULT_NUM_ITERATIONS)

        val keyAES = pGenAES.generateDerivedMacParameters(keyLenBytesEach * 8) as KeyParameter
        val keyTwofish = pGenTwofish.generateDerivedMacParameters(keyLenBytesEach * 8) as KeyParameter
        val keySerpent = pGenSerpent.generateDerivedMacParameters(keyLenBytesEach * 8) as KeyParameter

        passAsByteArray.wipe()

        return keyAES.key + keyTwofish.key + keySerpent.key
    }
}
