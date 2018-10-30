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

import org.bouncycastle.util.encoders.UrlBase64

data class KeyEntry(
        val id: Long,
        var name: String,
        var value: String,
        var encrypted: Boolean,
        var replacementKeyId: Long = 0,
        var revoked: Boolean = false
) {
    constructor(value: ByteArray)
            : this(
            id = -1,
            name = "",
            value = UrlBase64.encode(value).toString(charset = Charsets.UTF_8),
            encrypted = false
    )
}
