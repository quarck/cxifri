/*
 * Copyright (C) 2018 Sergey Parshin (quarck@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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