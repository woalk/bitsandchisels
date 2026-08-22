package com.woalk.mods.bitsandchisels.blocks

import com.mojang.serialization.Decoder
import com.mojang.serialization.Encoder
import com.mojang.serialization.MapCodec
import com.woalk.mods.bitsandchisels.registry.ModBlocks
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property
import net.minecraft.core.BlockPos
import net.minecraft.world.level.LevelReader

class ColoredBlockState(
    block: Block,
    propertyKeys: Array<Property<*>>,
    propertyValues: Array<Comparable<*>>,
    val wrapped: BlockState,
    val color: Int = 0xFFFFFF
) : BlockState(block, propertyKeys, propertyValues) {
    companion object {
        const val TAG_COLOR = "color"
    }

    constructor(state: BlockState, color: Int) :
            this(
                state.block,
                state.properties.toTypedArray(),
                state.properties.map { state.getValue(it as Property<Nothing>) as Comparable<*> }.toTypedArray(),
                state,
                color
            )

    constructor(state: BlockState, world: LevelReader, pos: BlockPos) :
            this(
                state,
                (world.getBlockEntity(pos) as ColoredBlockEntity).color
            )

    constructor(color: Int) : this(
        ModBlocks.coloredBlock.defaultBlockState(),
        color
    )

    override fun equals(other: Any?): Boolean {
        return if (other is ColoredBlockState) color == other.color else false
    }

    override fun hashCode(): Int {
        return color
    }
}
