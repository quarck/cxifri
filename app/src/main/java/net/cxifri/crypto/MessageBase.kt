package net.cxifri.crypto

open class MessageBase(val key: KeyEntry)

class TextMessage(key: KeyEntry, val text: String): MessageBase(key)

class KeyRevokeMessage(key: KeyEntry): MessageBase(key)