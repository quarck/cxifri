package com.github.quarck.kriptileto

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.KeyParameter

object DerivedKeyGenerator {

    val DEFAULT_NUM_ITERATIONS = 10000

    fun generateForAES(password: String): ByteArray {
        val pGen = PKCS5S2ParametersGenerator(SHA256Digest())
        pGen.init(
                password.toByteArray(charset = Charsets.UTF_8),
                "".toByteArray(charset = Charsets.UTF_8),
                DEFAULT_NUM_ITERATIONS
        )
        val keyLenBytes = 32
        val key = pGen.generateDerivedMacParameters(keyLenBytes * 8) as KeyParameter
        return key.key
    }

    fun generateForAESTwofishSerpent(password: String): ByteArray {
        val keyLenBytesEach = 32

        val saltAES = "cipher-AES".toByteArray(charset = Charsets.UTF_8)
        val saltTwofish = "cipher-Twofish".toByteArray(charset = Charsets.UTF_8)
        val saltSerpent = "cipher-Serpent".toByteArray(charset = Charsets.UTF_8)

        val pGenAES = PKCS5S2ParametersGenerator(SHA256Digest())
        val pGenTwofish = PKCS5S2ParametersGenerator(SHA256Digest())
        val pGenSerpent = PKCS5S2ParametersGenerator(SHA256Digest())

        val passAsByteArray = password.toByteArray(charset = Charsets.UTF_8)
        pGenAES.init(passAsByteArray, saltAES, DEFAULT_NUM_ITERATIONS)
        pGenTwofish.init(passAsByteArray, saltTwofish, DEFAULT_NUM_ITERATIONS)
        pGenSerpent.init(passAsByteArray, saltSerpent, DEFAULT_NUM_ITERATIONS)

        passAsByteArray.wipe()

        val keyAES = pGenAES.generateDerivedMacParameters(keyLenBytesEach * 8) as KeyParameter
        val keyTwofish = pGenAES.generateDerivedMacParameters(keyLenBytesEach * 8) as KeyParameter
        val keySerpent = pGenAES.generateDerivedMacParameters(keyLenBytesEach * 8) as KeyParameter

        return keyAES.key + keyTwofish.key + keySerpent.key
    }
}
