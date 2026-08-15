package com.woalk.mods.bitsandchisels.blocks

import com.google.common.collect.ImmutableMap
import com.mojang.serialization.Decoder
import com.mojang.serialization.Encoder
import com.mojang.serialization.MapCodec
import com.woalk.mods.bitsandchisels.registry.ModBlocks
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property
import net.minecraft.core.BlockPos
import net.minecraft.world.level.LevelReader

class ColoredBlockState(
    block: Block?,
    immutableMap: ImmutableMap<Property<*>, Comparable<*>>?,
    mapCodec: MapCodec<BlockState>?,
    val wrapped: BlockState,
    val color: Int = 0xFFFFFF
) : BlockState(block, immutableMap, mapCodec) {
    constructor(state: BlockState, world: LevelReader, pos: BlockPos) :
            this(
                state.block,
                state.values,
                MapCodec.of(Encoder.empty(), Decoder.unit { null }),
                state,
                (world.getBlockEntity(pos) as ColoredBlockEntity).color
            )

    constructor(color: Int) : this(
        ModBlocks.coloredBlock,
        ModBlocks.coloredBlock.defaultBlockState().values,
        MapCodec.of(Encoder.empty(), Decoder.unit { null }),
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
