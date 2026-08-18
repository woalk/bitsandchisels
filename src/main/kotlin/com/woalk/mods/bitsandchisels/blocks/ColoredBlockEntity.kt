package com.woalk.mods.bitsandchisels.blocks

import com.woalk.mods.bitsandchisels.registry.ModBlockEntities
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.DyedItemColor

class ColoredBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.coloredBlockEntity, pos, state) {
    var color: Int
        get() = components().get(DataComponents.DYED_COLOR)?.rgb ?: 0xFFFFFF
        set(value) {
            setComponents(DataComponentMap.builder().set(DataComponents.DYED_COLOR, DyedItemColor(value, true)).build())
        }
    var colorWithAlpha: Int
        get() = (0xFF000000U or color.toUInt()).toInt()
        set(value) {
            color = value and 0xFFFFFF
        }
}
