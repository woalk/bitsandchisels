package com.woalk.mods.bitsandchisels.blocks

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.BlockItem
import net.minecraft.item.DyeableItem
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class ColoredBlockItem(block: Block?, settings: Settings?) : BlockItem(block, settings), DyeableItem {
    override fun postPlacement(
        pos: BlockPos,
        world: World,
        player: PlayerEntity?,
        stack: ItemStack,
        state: BlockState
    ): Boolean {
        (world.getBlockEntity(pos) as ColoredBlockEntity).color =
            stack.nbt?.getCompound(DyeableItem.DISPLAY_KEY)?.getInt(DyeableItem.COLOR_KEY) ?: 0xFFFFFF
        return super.postPlacement(pos, world, player, stack, state)
    }
}
