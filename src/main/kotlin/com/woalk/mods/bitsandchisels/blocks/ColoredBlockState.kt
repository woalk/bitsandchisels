package com.woalk.mods.bitsandchisels.blocks

import com.google.common.collect.ImmutableMap
import com.mojang.serialization.MapCodec
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.state.property.Property

class ColoredBlockState(
    block: Block?,
    immutableMap: ImmutableMap<Property<*>, Comparable<*>>?,
    mapCodec: MapCodec<BlockState>?,
    private val color: UInt = 0xFFFFFFFFU
) : BlockState(block, immutableMap, mapCodec) {
    val colorInt get() = color.toInt()
}
