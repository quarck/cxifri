package com.github.quarck.kriptileto

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.KeyParameter

object DerivedKeyGenerator {

    val DEFAULT_NUM_ITERATIONS = 10000

    fun generate(password: String, salt: String, iterationCount: Int, keyLenBytes: Int): ByteArray? {
        val pGen = PKCS5S2ParametersGenerator(SHA256Digest())
        pGen.init(
                password.toByteArray(charset = Charsets.UTF_8),
                salt.toByteArray(charset = Charsets.UTF_8),
                if (iterationCount != 0) iterationCount else DEFAULT_NUM_ITERATIONS
        )
        val key = pGen.generateDerivedMacParameters(keyLenBytes * 8) as KeyParameter
        return key.key
    }
}
