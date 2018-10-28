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

import net.cxifri.dataprocessing.UrlWrapper


class UrlWrappedMessageHandler(val messageHandler: MessageHandlerInterface): MessageHandlerInterface {

    override fun encrypt(message: MessageBase, key: KeyEntry): String {
        return UrlWrapper.wrap(messageHandler.encrypt(message, key))
    }

    override fun decrypt(message: String, key: KeyEntry): MessageBase? {
        val unwrapped = UrlWrapper.unwrapOrKillSpaces(message)
        return messageHandler.decrypt(unwrapped, key)
    }

    override fun decrypt(message: String, keys: List<KeyEntry>): MessageBase? {
        val unwrapped = UrlWrapper.unwrapOrKillSpaces(message)
        return messageHandler.decrypt(unwrapped, keys)
    }
}