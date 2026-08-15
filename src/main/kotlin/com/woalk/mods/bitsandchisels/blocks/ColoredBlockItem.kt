package com.woalk.mods.bitsandchisels.blocks

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.DyeableLeatherItem
import net.minecraft.world.item.ItemStack
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

class ColoredBlockItem(block: Block?, settings: Properties?) : BlockItem(block, settings), DyeableLeatherItem {
    override fun updateCustomBlockEntityTag(
        pos: BlockPos,
        world: Level,
        player: Player?,
        stack: ItemStack,
        state: BlockState
    ): Boolean {
        (world.getBlockEntity(pos) as ColoredBlockEntity).color =
            stack.tag?.getCompound(DyeableLeatherItem.TAG_DISPLAY)?.getInt(DyeableLeatherItem.TAG_COLOR) ?: 0xFFFFFF
        return super.updateCustomBlockEntityTag(pos, world, player, stack, state)
    }
}
