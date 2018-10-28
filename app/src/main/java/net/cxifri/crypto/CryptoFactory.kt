package net.cxifri.crypto

import org.bouncycastle.util.Strings
import org.bouncycastle.util.encoders.UrlBase64
import java.nio.charset.Charset

object CryptoFactory {
    fun createMessageHandler(): MessageHandlerInterface {
        return UrlWrappedMessageHandler(
                MessageHandler(
                        BinaryMessageHandler(
                                createEngine = { AESTwofishSerpentEngine() }
                        )))
    }

    fun deriveKeyFromPassword(password: String): KeyEntry {
        val bytes = DerivedKeyGenerator().generateForAESTwofishSerpent(password)
        return KeyEntry(
                id = -1,
                name="\$\$TEMP\$\$",
                value = UrlBase64.encode(bytes).toString(charset =  Charsets.UTF_8),
                encrypted = false
        )
    }
}