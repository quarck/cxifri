package com.github.quarck.kriptileto.utils

fun ByteArray.wipe() {
    for (i in 0 until size)
        this[i] = 0
}
