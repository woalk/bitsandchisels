package com.woalk.mods.bitsandchisels.image

import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.util.registry.Registry
import java.awt.image.IndexColorModel

@OptIn(ExperimentalUnsignedTypes::class)
class ConcreteColorModel(val withSkinColor: Boolean = false) : IndexColorModel(
    5,
    17,
    /*r*/
    ubyteArrayOf(255U,   0U,  45U,  97U,  21U,  55U,  74U,  36U, 125U,  94U, 169U, 225U, 213U, 101U, 142U, 208U, 242U)
        .toByteArray() + if (withSkinColor) ubyteArrayOf(255U, 224U).toByteArray() else byteArrayOf(),
    /*g*/
    ubyteArrayOf(255U,   0U,  47U,  60U, 118U,  58U,  92U, 138U, 125U, 169U,  48U,  99U, 100U,  33U,  32U, 214U, 177U)
        .toByteArray() + if (withSkinColor) ubyteArrayOf(219U, 172U).toByteArray() else byteArrayOf(),
    /*b*/
    ubyteArrayOf(255U,   0U, 144U,  32U, 135U,  62U,  38U, 200U, 115U,  24U, 159U,   3U, 132U, 157U,  32U, 215U,  23U)
        .toByteArray() + if (withSkinColor) ubyteArrayOf(172U, 105U).toByteArray() else byteArrayOf(),
    0
) {
    enum class Color {
        TRANSPARENT,
        BLACK,
        BLUE,
        BROWN,
        CYAN,
        GRAY,
        GREEN,
        LIGHT_BLUE,
        LIGHT_GRAY,
        LIME,
        MAGENTA,
        ORANGE,
        PINK,
        PURPLE,
        RED,
        WHITE,
        YELLOW,
        LIGHT_SKIN,
        TAN_SKIN;

        fun toBlock(palette: PaletteIdentifier = PaletteIdentifier(), transparent: Boolean = false): Block =
            if (this == TRANSPARENT) {
                if (transparent) {
                    Blocks.AIR
                } else {
                    Registry.BLOCK.get(palette.toIdentifier(WHITE.name.lowercase()))
                }
            } else {
                Registry.BLOCK.get(palette.toIdentifier(name.lowercase()))
            }
    }
}
