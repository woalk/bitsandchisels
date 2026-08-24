package com.woalk.mods.bitsandchisels.utils

import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.EmptyBlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState

object Heuristic {
    fun isFullBlock(
        block: BlockState,
        level: BlockGetter = EmptyBlockGetter.INSTANCE,
        pos: BlockPos = BlockPos.ZERO,
    ) = !(
            block.renderShape != RenderShape.MODEL ||
            block.block.hasDynamicShape() ||
            block.useShapeForLightOcclusion() ||
            block.block is FaceAttachedHorizontalDirectionalBlock ||
            block.block is LeavesBlock ||
            !Block.isShapeFullBlock(block.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) ||
            !block.isCollisionShapeFullBlock(level, pos)
        )
}