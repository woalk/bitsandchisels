package com.woalk.mods.bitsandchisels.blocks

import com.woalk.mods.bitsandchisels.BitsBlock
import com.woalk.mods.bitsandchisels.ItemHelpers
import com.woalk.mods.bitsandchisels.registry.ModBlockEntities
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader

class AnimatedBitsBlock(settings: Properties?) : BitsBlock(settings) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = AnimatedBitsBlockEntity(pos, state)

    override fun getRenderShape(state: BlockState?): RenderShape = RenderShape.ENTITYBLOCK_ANIMATED

    override fun playerWillDestroy(world: Level, pos: BlockPos?, state: BlockState?, player: Player): BlockState {
        val blockEntity = world.getBlockEntity(pos, ModBlockEntities.animatedBitsBlockEntity).orElse(null)
        popResource(world, pos, ItemHelpers.blockToItem(this, state, blockEntity))
        return playerWillDestroyBlock(world, pos, state, player)
    }

    override fun getCloneItemStack(world: LevelReader, pos: BlockPos, state: BlockState): ItemStack? {
        val blockEntity = world.getBlockEntity(pos, ModBlockEntities.animatedBitsBlockEntity).orElse(null)
        return if (blockEntity != null) {
            ItemHelpers.blockToItem(this, state, blockEntity)
        } else {
            ItemStack.EMPTY
        }
    }

    override fun <T : BlockEntity?> getTicker(
        world: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (!world.isClientSide) {
            return null
        }
        return createTickerHelper(type, ModBlockEntities.animatedBitsBlockEntity) { _, _, _, be ->
            be.tick()
        }
    }
}
