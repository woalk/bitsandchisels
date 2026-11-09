package com.woalk.mods.bitsandchisels.blocks

import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.world.level.LevelReader

class ColoredBlock(settings: Properties) : BaseEntityBlock(settings) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState) = ColoredBlockEntity(pos, state)

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Copied from superclass")
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun getCloneItemStack(world: LevelReader, pos: BlockPos, state: BlockState, includeData: Boolean) =
        super.getCloneItemStack(world, pos, state, includeData)!!.apply {
            val entity = world.getBlockEntity(pos) as ColoredBlockEntity
            val color = entity.components().get(DataComponents.DYED_COLOR)
            set(DataComponents.DYED_COLOR, color)
        }
}