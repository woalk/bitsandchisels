package com.woalk.mods.bitsandchisels.networking

import com.woalk.mods.bitsandchisels.BitsAndChisels
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

abstract class ChiselPacket(
    val blockPos: BlockPos,
    val bitPos: BlockPos,
): CustomPacketPayload {
    companion object {
        @JvmStatic
        protected fun <T: ChiselPacket> getTypeForPacket(id: String) =
            CustomPacketPayload.Type<T>(ResourceLocation(BitsAndChisels.MODID, id))
    }

    constructor(buf: RegistryFriendlyByteBuf): this(buf.readBlockPos(), buf.readBlockPos())

    open fun write(buf: RegistryFriendlyByteBuf) {
        buf.writeBlockPos(blockPos)
        buf.writeBlockPos(bitPos)
    }
}
