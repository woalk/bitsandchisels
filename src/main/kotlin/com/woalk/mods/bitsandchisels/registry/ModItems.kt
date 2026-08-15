package com.woalk.mods.bitsandchisels.registry

import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.blocks.BitStructureItem
import com.woalk.mods.bitsandchisels.chisel.AnimationWand
import com.woalk.mods.bitsandchisels.chisel.DuplicationKit
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.world.item.Item
import net.minecraft.core.Registry

@Suppress("unused")
object ModItems: ModRegister<Item, Item>(Registry.ITEM) {
    private fun itemSettings() = FabricItemSettings().tab(BitsAndChisels.ITEM_GROUP)

    //region item declarations
    val animationWand = register("animation_wand", AnimationWand(itemSettings()))
    val bitStructureItem = register("bit_structure", BitStructureItem(FabricItemSettings().stacksTo(16)))
    val duplicationKit = register("duplication_kit", DuplicationKit(itemSettings().stacksTo(16)))
    //endregion

    //region convenience functions
    override fun onRegister(toRegister: Item): Item = toRegister
    //endregion
}