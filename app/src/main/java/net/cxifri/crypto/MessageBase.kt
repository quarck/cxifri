package net.cxifri.crypto

open class MessageBase(val key: KeyEntry)

class TextMessage(key: KeyEntry, val text: String): MessageBase(key)

class KeyReplacementMessage(key: KeyEntry, val newKey: KeyEntry, val receiverMustDeleteOldKey: Boolean): MessageBase(key)

class KeyRevokeMessage(key: KeyEntry): MessageBase(key)