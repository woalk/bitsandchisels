package com.woalk.mods.bitsandchisels.blocks

import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.item.DyeableLeatherItem
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter

class ColoredBlock(settings: Properties?) : BaseEntityBlock(settings) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState) = ColoredBlockEntity(pos, state)

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Copied from superclass")
    override fun getRenderShape(state: BlockState?): RenderShape = RenderShape.MODEL

    override fun getCloneItemStack(world: BlockGetter, pos: BlockPos, state: BlockState): ItemStack =
        super.getCloneItemStack(world, pos, state).apply {
            val entity = world.getBlockEntity(pos) as ColoredBlockEntity
            val newNbt = CompoundTag()
            val display = CompoundTag()
            display.putInt(DyeableLeatherItem.TAG_COLOR, entity.color)
            newNbt.put(DyeableLeatherItem.TAG_DISPLAY, display)
            setTag(newNbt)
        }
}