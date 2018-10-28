package net.cxifri.crypto

interface MessageHandlerInterface {

    fun encrypt(message: MessageBase, key: KeyEntry): String
    fun decrypt(message: String, key: KeyEntry): MessageBase?
    fun decrypt(message: String, keys: List<KeyEntry>): MessageBase?
}