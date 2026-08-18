package com.woalk.mods.bitsandchisels.networking

import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

class BitPacket : Chisel2Packet {
    companion object {
        @JvmField
        val TYPE = getTypeForPacket<BitPacket>("bit")
        private val CODEC = StreamCodec.ofMember(BitPacket::write, ::BitPacket)

        fun register() = Pair(TYPE, CODEC)
    }

    val isMainHandInteraction: Boolean

    constructor(
        blockPos: BlockPos,
        bitPos: BlockPos,
        bitPos2: BlockPos,
        isMainHandInteraction: Boolean = true,
    ) : super(blockPos, bitPos, bitPos2) {
        this.isMainHandInteraction = isMainHandInteraction
    }

    constructor(buf: RegistryFriendlyByteBuf) : super(buf) {
        isMainHandInteraction = buf.readBoolean()
    }

    override fun type() = TYPE

    override fun write(buf: RegistryFriendlyByteBuf) {
        super.write(buf)
        buf.writeBoolean(isMainHandInteraction)
    }
}