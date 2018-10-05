package com.github.quarck.kriptileto

import java.security.SecureRandom

object RandomKeyGenerator {
    val random: SecureRandom by lazy { SecureRandom() }

    fun generate(lenBytes: Int): ByteArray {
        val key = ByteArray(lenBytes)
        random.nextBytes(key)
        return key
    }
}