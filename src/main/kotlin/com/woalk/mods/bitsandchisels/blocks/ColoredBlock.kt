package com.woalk.mods.bitsandchisels.blocks

import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.item.DyeableItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockView

class ColoredBlock(settings: Settings?) : BlockWithEntity(settings) {
    override fun createBlockEntity(pos: BlockPos, state: BlockState) = ColoredBlockEntity(pos, state)

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Copied from superclass")
    override fun getRenderType(state: BlockState?): BlockRenderType = BlockRenderType.MODEL

    override fun getPickStack(world: BlockView, pos: BlockPos, state: BlockState): ItemStack =
        super.getPickStack(world, pos, state).apply {
            val entity = world.getBlockEntity(pos) as ColoredBlockEntity
            val newNbt = NbtCompound()
            val display = NbtCompound()
            display.putInt(DyeableItem.COLOR_KEY, entity.color)
            newNbt.put(DyeableItem.DISPLAY_KEY, display)
            nbt = newNbt
        }
}