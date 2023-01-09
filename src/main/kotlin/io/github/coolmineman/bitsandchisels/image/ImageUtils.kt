package io.github.coolmineman.bitsandchisels.image

import java.awt.image.BufferedImage

fun BufferedImage.hasTransparentPixel(): Boolean {
    if (type != BufferedImage.TYPE_4BYTE_ABGR) {
        return false
    }
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (getRGB(x, y).shr(24) == 0x00) {
                return true
            }
        }
    }
    return false
}
