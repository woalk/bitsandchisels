package com.woalk.mods.bitsandchisels.networking

import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

class IronChiselPacket: ChiselPacket {
    companion object {
        @JvmField
        val TYPE = getTypeForPacket<IronChiselPacket>("iron_chisel")
        private val CODEC = StreamCodec.ofMember(IronChiselPacket::write, ::IronChiselPacket)

        fun register() = Pair(TYPE, CODEC)
    }

    constructor(blockPos: BlockPos, bitPos: BlockPos): super(blockPos, bitPos)
    constructor(buf: RegistryFriendlyByteBuf): super(buf)

    override fun type() = TYPE
}