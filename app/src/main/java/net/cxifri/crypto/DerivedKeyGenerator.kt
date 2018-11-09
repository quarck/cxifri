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

import net.cxifri.utils.wipe
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.KeyParameter

class DerivedKeyGenerator {

    fun generateFromTextPassword(password: String, keyLenBytes: Int, name: String=""): KeyEntry {

        val passwordBytes = password.toByteArray(charset = Charsets.UTF_8)

        val pkcsGen = PKCS5S2ParametersGenerator(SHA256Digest()).apply{
            init(passwordBytes, SALT_TEXT, NUM_ITERATIONS)
        }
        val param = pkcsGen.generateDerivedParameters(keyLenBytes * 8) as KeyParameter
        val ret = KeyEntry(param.key, name)

        passwordBytes.wipe()
        return ret
    }

    companion object {

        const val NUM_ITERATIONS = 100000
        val SALT_TEXT = "cxifri-text-pass-/exgD19HBcMPArzKeBdvptTg5SXjG1nh9w0CgF".toByteArray(charset= Charsets.UTF_8)
    }
}
