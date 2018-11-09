package net.cxifri.crypto

import org.bouncycastle.util.Strings
import org.bouncycastle.util.encoders.UrlBase64
import java.nio.charset.Charset

object CryptoFactory {
    val secretGenerator = RandomSharedSecretGenerator()

    fun createMessageHandler(): MessageHandlerInterface {
        return UrlWrappedMessageHandler(MessageHandler(BinaryMessageHandler()))
    }

    fun deriveKeyFromPassword(password: String, name: String): KeyEntry {
        return DerivedKeyGenerator().generateFromTextPassword(password, AESTwofishSerpentEngine.KEY_LENGTH, name)
    }

    fun generateRandomKeyWithCsum(): Pair<ByteArray, ByteArray> {
        return secretGenerator.generateKeywithCSum(AESTwofishSerpentEngine.KEY_LENGTH)
    }

    fun generateRandomKey(): ByteArray {
        return secretGenerator.generate(AESTwofishSerpentEngine.KEY_LENGTH)
    }
}