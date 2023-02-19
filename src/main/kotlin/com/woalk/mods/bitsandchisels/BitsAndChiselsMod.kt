package com.woalk.mods.bitsandchisels

import com.woalk.mods.bitsandchisels.utils.DirectionArgumentType
import com.woalk.mods.bitsandchisels.compat.WorldEditCompat
import com.woalk.mods.bitsandchisels.registry.ModCommands
import net.fabricmc.loader.api.FabricLoader

object BitsAndChiselsMod {
    fun onInitialize() {
        ModCommands.registerAll()
        if (FabricLoader.getInstance().isModLoaded("worldedit")) {
            DirectionArgumentType.registerDirectionArgumentType()
            WorldEditCompat.registerAll()
        }
    }
}