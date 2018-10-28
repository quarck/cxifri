package net.cxifri.crypto

open class MessageBase()

class TextMessage(val key: KeyEntry, val text: String): MessageBase()

class KeyReplacementMessage(val key: KeyEntry, val newKey: String): MessageBase()

class KeyRevokeMessage(val key: KeyEntry): MessageBase()