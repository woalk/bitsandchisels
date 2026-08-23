package com.woalk.mods.bitsandchisels.registry

import com.woalk.mods.bitsandchisels.BitsAndChisels
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey

object ModTagKeys {
    val chisel
        get() = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(BitsAndChisels.MODID, "chisel"))
}