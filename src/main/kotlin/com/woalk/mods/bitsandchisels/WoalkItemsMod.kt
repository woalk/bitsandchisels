package com.woalk.mods.bitsandchisels

import net.fabricmc.loader.api.FabricLoader
import java.io.File

object WoalkItemsMod {
    const val MOD_ID = "woalk"

    val isAdmin by lazy {
        val configFile = File(FabricLoader.getInstance().configDir.toFile(), "woalk.conf")
        val config = if (configFile.exists()) {
            configFile.readText()
        } else null
        return@lazy config?.contains("showItemsInCreativeMenu=true") ?: false
    }
}