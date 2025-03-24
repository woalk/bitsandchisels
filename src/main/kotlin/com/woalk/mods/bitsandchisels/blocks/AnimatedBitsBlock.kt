package com.woalk.mods.bitsandchisels.blocks

import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.BitsBlock
import com.woalk.mods.bitsandchisels.ItemHelpers
import com.woalk.mods.bitsandchisels.registry.ModBlockEntities
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockView
import net.minecraft.world.World

class AnimatedBitsBlock(settings: Settings?) : BitsBlock(settings) {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = AnimatedBitsBlockEntity(pos, state)

    override fun getRenderType(state: BlockState?): BlockRenderType = BlockRenderType.ENTITYBLOCK_ANIMATED

    override fun onBreak(world: World, pos: BlockPos?, state: BlockState?, player: PlayerEntity) {
        val blockEntity = world.getBlockEntity(pos, ModBlockEntities.animatedBitsBlockEntity).orElse(null)
        dropStack(world, pos, ItemHelpers.blockToItem(this, state, blockEntity))
    }

    override fun getPickStack(world: BlockView, pos: BlockPos?, state: BlockState?): ItemStack {
        val blockEntity = world.getBlockEntity(pos, ModBlockEntities.animatedBitsBlockEntity).orElse(null)
        return if (blockEntity != null) {
            ItemHelpers.blockToItem(this, state, blockEntity)
        } else {
            ItemStack.EMPTY
        }
    }

    override fun <T : BlockEntity?> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (!world.isClient) {
            return null
        }
        return checkType(type, ModBlockEntities.animatedBitsBlockEntity) { _, _, _, be ->
            be.tick()
        }
    }
}
