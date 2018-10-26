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
