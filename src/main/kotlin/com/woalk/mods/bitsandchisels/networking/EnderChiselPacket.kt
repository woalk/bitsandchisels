package com.woalk.mods.bitsandchisels.networking

import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

class EnderChiselPacket: ChiselPacket {
    companion object {
        @JvmField
        val TYPE = getTypeForPacket<EnderChiselPacket>("ender_chisel")
        private val CODEC = StreamCodec.ofMember(EnderChiselPacket::write, ::EnderChiselPacket)!!

        fun register() = Pair(TYPE, CODEC)
    }

    constructor(blockPos: BlockPos, bitPos: BlockPos): super(blockPos, bitPos)
    constructor(buf: RegistryFriendlyByteBuf): super(buf)

    override fun type() = TYPE
}