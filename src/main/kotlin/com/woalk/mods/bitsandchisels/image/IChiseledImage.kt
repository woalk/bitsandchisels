package com.woalk.mods.bitsandchisels.image

import net.minecraft.world.level.block.state.BlockState

interface IChiseledImage {
    val width: Int
    val height: Int
    val blockWidth: Int
    val blockHeight: Int

    fun getImageName(): String? = null
    fun getStateForPixel(x: Int, y: Int, palette: PaletteIdentifier? = null): BlockState
}