package com.woalk.mods.bitsandchisels.blocks

import com.woalk.mods.bitsandchisels.registry.ModBlockEntities
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.item.DyeableItem
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.Packet
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.util.math.BlockPos

class ColoredBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.coloredBlockEntity, pos, state) {
    var color: Int = 0xFFFFFF
    var colorWithAlpha: Int
        get() = (0xFF000000U or color.toUInt()).toInt()
        set(value) {
            color = value and 0xFFFFFF
        }

    override fun writeNbt(nbt: NbtCompound) {
        nbt.putInt(DyeableItem.COLOR_KEY, color)
        super.writeNbt(nbt)
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        if (nbt.contains(DyeableItem.COLOR_KEY)) {
            color = nbt.getInt(DyeableItem.COLOR_KEY)
        }
    }

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener>? = BlockEntityUpdateS2CPacket.create(this)

    override fun toInitialChunkDataNbt(): NbtCompound = createNbt()
}
