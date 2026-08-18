package com.woalk.mods.bitsandchisels

import com.woalk.mods.bitsandchisels.utils.DirectionArgumentType
import com.woalk.mods.bitsandchisels.compat.WorldEditCompat
import com.woalk.mods.bitsandchisels.registry.ModBlockEntities
import com.woalk.mods.bitsandchisels.registry.ModBlocks
import com.woalk.mods.bitsandchisels.registry.ModCommands
import com.woalk.mods.bitsandchisels.registry.ModItems
import com.woalk.mods.bitsandchisels.registry.ModPackets
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader

object BitsAndChiselsMod: ModInitializer {
    override fun onInitialize() {
        ModPackets.registerAll()
        ModCommands.registerAll()
        ModBlocks.registerAll()
        ModBlockEntities.registerAll()
        ModItems.registerAll()
        if (FabricLoader.getInstance().isModLoaded("worldedit")) {
            DirectionArgumentType.registerDirectionArgumentType()
            WorldEditCompat.registerAll()
        }
    }
}