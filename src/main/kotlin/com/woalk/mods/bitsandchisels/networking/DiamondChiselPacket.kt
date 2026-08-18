package com.woalk.mods.bitsandchisels.networking

import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

class DiamondChiselPacket: ChiselPacket {
    companion object {
        @JvmField
        val TYPE = getTypeForPacket<DiamondChiselPacket>("diamond_chisel")
        private val CODEC = StreamCodec.ofMember(DiamondChiselPacket::write, ::DiamondChiselPacket)

        fun register() = Pair(TYPE, CODEC)
    }

    constructor(blockPos: BlockPos, bitPos: BlockPos): super(blockPos, bitPos)
    constructor(buf: RegistryFriendlyByteBuf): super(buf)

    override fun type() = TYPE
}