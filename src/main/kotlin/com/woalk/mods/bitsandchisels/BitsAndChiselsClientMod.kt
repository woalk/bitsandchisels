package com.woalk.mods.bitsandchisels

import com.woalk.mods.bitsandchisels.registry.ModBlocks
import net.fabricmc.api.ClientModInitializer

object BitsAndChiselsClientMod: ClientModInitializer {
    override fun onInitializeClient() {
        ModBlocks.registerAllClient()
    }
}