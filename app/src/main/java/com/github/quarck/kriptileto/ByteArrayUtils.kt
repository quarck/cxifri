package com.github.quarck.kriptileto

fun ByteArray.wipe() {
    for (i in 0 until size)
        this[i] = 0
}
