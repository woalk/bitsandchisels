package com.woalk.mods.bitsandchisels.blocks

import com.woalk.mods.bitsandchisels.registry.ModBlockEntities
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.item.DyeableLeatherItem
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.core.BlockPos

class ColoredBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.coloredBlockEntity, pos, state) {
    var color: Int = 0xFFFFFF
    var colorWithAlpha: Int
        get() = (0xFF000000U or color.toUInt()).toInt()
        set(value) {
            color = value and 0xFFFFFF
        }

    override fun saveAdditional(nbt: CompoundTag) {
        nbt.putInt(DyeableLeatherItem.TAG_COLOR, color)
        super.saveAdditional(nbt)
    }

    override fun load(nbt: CompoundTag) {
        super.load(nbt)
        if (nbt.contains(DyeableLeatherItem.TAG_COLOR)) {
            color = nbt.getInt(DyeableLeatherItem.TAG_COLOR)
        }
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? = ClientboundBlockEntityDataPacket.create(this)

    override fun getUpdateTag(): CompoundTag = saveWithoutMetadata()
}
