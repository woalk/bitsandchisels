package com.woalk.mods.bitsandchisels.networking

import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf

abstract class Chisel2Packet: ChiselPacket {
    val bitPos2: BlockPos

    constructor(blockPos: BlockPos, bitPos: BlockPos, bitPos2: BlockPos): super(blockPos, bitPos) {
        this.bitPos2 = bitPos2
    }
    constructor(buf: RegistryFriendlyByteBuf): super(buf) {
        bitPos2 = buf.readBlockPos()
    }

    override fun write(buf: RegistryFriendlyByteBuf) {
        super.write(buf)
        buf.writeBlockPos(bitPos2)
    }
}