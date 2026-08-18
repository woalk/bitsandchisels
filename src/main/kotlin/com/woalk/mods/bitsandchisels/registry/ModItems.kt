package com.woalk.mods.bitsandchisels.registry

import com.woalk.mods.bitsandchisels.BitItem
import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.blocks.BitStructureItem
import com.woalk.mods.bitsandchisels.blocks.BitsBlockItem
import com.woalk.mods.bitsandchisels.chisel.*
import com.woalk.mods.bitsandchisels.wrench.WrenchItem
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item

@Suppress("unused")
object ModItems: ModRegister<Item, Item>(BuiltInRegistries.ITEM) {
    private fun itemSettings() = Item.Properties()

    //region item declarations
    @JvmField
    val BIT_ITEM = registerTyped("bit_item", BitItem(Item.Properties()))
    @JvmField
    val DIAMOND_CHISEL = registerTyped("diamond_chisel", DiamondChisel(itemSettings()))
    @JvmField
    val ENDER_CHISEL = registerTyped("ender_chisel", EnderChisel(itemSettings()))
    @JvmField
    val IRON_CHISEL = registerTyped("iron_chisel", IronChisel(itemSettings()))
    @JvmField
    val SMART_CHISEL = registerTyped("smart_chisel", SmartChisel(itemSettings()))
    @JvmField
    val WRENCH_ITEM = registerTyped("wrench", WrenchItem(itemSettings()))
    @JvmField
    val BITS_BLOCK_ITEM = registerTyped("bits_block", BitsBlockItem(BitsAndChisels.BITS_BLOCK, Item.Properties()))

    val animationWand = register("animation_wand", AnimationWand(itemSettings()))
    val bitStructureItem = register("bit_structure", BitStructureItem(Item.Properties().stacksTo(16)))
    val duplicationKit = register("duplication_kit", DuplicationKit(itemSettings().stacksTo(16)))
    //endregion

    //region convenience functions
    @Suppress("UNCHECKED_CAST")
    fun <T: Item> registerTyped(name: String, item: T): T = register(name, item) as T

    override fun onRegister(toRegister: Item): Item = toRegister
    //endregion
}