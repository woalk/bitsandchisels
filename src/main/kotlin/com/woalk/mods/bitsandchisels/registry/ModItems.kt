package com.woalk.mods.bitsandchisels.registry

import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.blocks.BitStructureItem
import com.woalk.mods.bitsandchisels.chisel.AnimationWand
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.Item
import net.minecraft.util.registry.Registry

@Suppress("unused")
object ModItems: ModRegister<Item, Item>(Registry.ITEM) {
    private val itemSettings = FabricItemSettings().group(BitsAndChisels.ITEM_GROUP)

    //region item declarations
    val animationWand = register("animation_wand", AnimationWand(itemSettings))
    val bitStructureItem = register("bit_structure", BitStructureItem(FabricItemSettings()))
    //endregion

    //region convenience functions
    override fun onRegister(toRegister: Item): Item = toRegister
    //endregion
}