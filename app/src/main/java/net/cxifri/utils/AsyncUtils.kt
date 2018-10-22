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


package net.cxifri.utils

// Anko is not building properly for me for some reason, so
// it was easier to just create a few lines of code below rather then
// investigating why it is not working

import android.os.AsyncTask

class AsyncOperation(val fn: () -> Unit)
    : AsyncTask<Void?, Void?, Void?>() {
    override fun doInBackground(vararg p0: Void?): Void? {
        fn()
        return null
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun background(noinline fn: () -> Unit) {
    AsyncOperation(fn).execute();
}

