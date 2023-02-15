package io.github.coolmineman.bitsandchisels

import io.github.coolmineman.bitsandchisels.compat.WorldEditCompat
import io.github.coolmineman.bitsandchisels.registry.ModCommands
import net.fabricmc.loader.api.FabricLoader

object BitsAndChiselsMod {
    fun onInitialize() {
        ModCommands.registerAll()
        if (FabricLoader.getInstance().isModLoaded("worldedit")) {
            WorldEditCompat.registerAll()
        }
    }
}