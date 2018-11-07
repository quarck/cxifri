package net.cxifri.crypto

interface BinaryMessageHandlerInterface {
    fun encrypt(message: ByteArray, key: ByteArray): ByteArray
    fun decrypt(message: ByteArray, key: ByteArray): ByteArray?
}