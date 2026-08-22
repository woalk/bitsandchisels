package com.woalk.mods.bitsandchisels.registry

import com.woalk.mods.bitsandchisels.networking.BitPacket
import com.woalk.mods.bitsandchisels.networking.DiamondChiselPacket
import com.woalk.mods.bitsandchisels.networking.EnderChiselPacket
import com.woalk.mods.bitsandchisels.networking.IronChiselPacket
import com.woalk.mods.bitsandchisels.networking.SmartChiselPacket
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

object ModPackets: IModRegister {
    override fun registerAll() {
        PayloadTypeRegistry.serverboundPlay().run {
            register(EnderChiselPacket.register())
            register(DiamondChiselPacket.register())
            register(IronChiselPacket.register())
            register(SmartChiselPacket.register())
            register(BitPacket.register())
        }
    }

    /**
     * Registers a payload type and its corresponding stream codec within a PayloadTypeRegistry.
     * This is a shortcut method for [PayloadTypeRegistry.register],
     * to be used with Kotlin companion functions in each packet for convenience.
     *
     * @param toRegister A pair consisting of the [CustomPacketPayload.Type] to be registered
     *                   and the corresponding [StreamCodec] implementation to handle serialization
     *                   and deserialization for the specified payload type.
     * @param T The type of the [CustomPacketPayload] being registered.
     * @param B The type of the buffer used for encoding/decoding payloads.
     */
    private fun <T: CustomPacketPayload, B: FriendlyByteBuf> PayloadTypeRegistry<B>.register(
        toRegister: Pair<CustomPacketPayload.Type<T>, StreamCodec<B, T>>
    ) = register(toRegister.first, toRegister.second)
}
