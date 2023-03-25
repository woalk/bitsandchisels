package com.woalk.mods.bitsandchisels.utils

import java.awt.Color

fun String?.toColor(): Int? = try {
    if (this == null) -1
    else if (length == 3) {
        val r = this[0].digitToInt(16)
        val g = this[1].digitToInt(16)
        val b = this[2].digitToInt(16)
        (r shl 20) or (r shl 16) or (g shl 12) or (g shl 8) or (b shl 4) or b
    } else if (length == 6) {
        val color = Color.decode("#$this")
        (color.red shl 16) or (color.green shl 8) or color.blue
    } else -1
} catch (e: NumberFormatException) {
    -1
}
