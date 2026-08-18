package com.woalk.mods.bitsandchisels.networking

import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

open class SmartChiselPacket: Chisel2Packet {
    companion object {
        @JvmField
        val TYPE = getTypeForPacket<SmartChiselPacket>("smart_chisel")
        private val CODEC = StreamCodec.ofMember(SmartChiselPacket::write, ::SmartChiselPacket)

        fun register() = Pair(TYPE, CODEC)
    }

    constructor(blockPos: BlockPos, bitPos: BlockPos, bitPos2: BlockPos): super(blockPos, bitPos, bitPos2)
    constructor(buf: RegistryFriendlyByteBuf): super(buf)

    override fun type() = TYPE
}