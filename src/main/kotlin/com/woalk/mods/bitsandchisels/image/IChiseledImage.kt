package com.woalk.mods.bitsandchisels.image

import net.minecraft.block.BlockState

interface IChiseledImage {
    val width: Int
    val height: Int
    val blockWidth: Int
    val blockHeight: Int

    fun getImageName(): String? = null
    fun getStateForPixel(x: Int, y: Int, palette: PaletteIdentifier? = null): BlockState
}