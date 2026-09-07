package com.woalk.mods.bitsandchisels.compat

import com.woalk.mods.datafixer.ConfigReader
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.nbt.CompoundTag

object WoalkDatafixerCompat {
    @JvmStatic
    fun isLoaded(): Boolean {
        return FabricLoader.getInstance().isModLoaded("woalk-datafixer") || FabricLoader.getInstance().isModLoaded("wdf")
    }

    private val blockMapping by lazy {
        ConfigReader.blockMapping()
    }

    @JvmStatic
    fun replaceBlockState(tag: CompoundTag): CompoundTag {
        val name = tag.getString("Name").orElse(null) ?: return tag
        val replacement = blockMapping[name] ?: return tag
        val newTag = tag.copy()
        newTag.putString("Name", replacement)
        return newTag
    }
}
