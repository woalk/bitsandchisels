package com.woalk.mods.bitsandchisels.blocks

import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.TransparentBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

class OcclusionBlock(properties: Properties) : TransparentBlock(properties) {
    companion object {
        @JvmStatic
        val OCCLUDES = BooleanProperty.create("occludes")
    }

    init {
        registerDefaultState(defaultBlockState().setValue(OCCLUDES, false))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(OCCLUDES)
    }

    override fun getVisualShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        return if (state.getValue(OCCLUDES)) Shapes.block() else super.getVisualShape(state, level, pos, context)
    }

    override fun getOcclusionShape(state: BlockState): VoxelShape {
        return if (state.getValue(OCCLUDES)) Shapes.block() else Shapes.empty()
    }
}