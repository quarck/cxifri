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
        return DerivedKeyGenerator().generateFromTextPassword(password)
    }

//    fun deriveKeyFromSharedSecret(secret: ByteArray): KeyEntry {
//        return DerivedKeyGenerator().generateFromSharedSecret(secret, "")
//    }
}