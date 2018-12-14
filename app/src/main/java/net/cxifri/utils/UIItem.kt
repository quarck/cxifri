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

import android.support.v7.app.AppCompatActivity
import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import kotlin.reflect.KProperty

class UIItem<T: View>(val resId: Int) {

    var field: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val activity = (thisRef as AppCompatActivity)
        if (field == null) {
            field = activity.findViewById<T>(resId)
        }
        return field ?: throw Exception("Cannot find resource ID $resId for property name ${property.name}")
    }
}

class ViewItem<T: View>(val root: View, val resId: Int) {

    var field: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (field == null) {
            field = root.findViewById<T>(resId)
        }
        return field ?: throw Exception("Cannot find resource ID $resId for property name ${property.name}")
    }
}
