package net.cxifri.utils

import android.support.v7.app.AppCompatActivity
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
