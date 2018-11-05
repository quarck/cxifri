package net.cxifri.crypto

interface BinaryMessageHandlerInterface {
    fun encrypt(message: ByteArray, textKey: ByteArray, authKey: ByteArray): ByteArray
    fun decrypt(message: ByteArray, textKey: ByteArray, authKey: ByteArray): ByteArray?
}