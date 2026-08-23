package com.woalk.mods.bitsandchisels.registry

import com.woalk.mods.bitsandchisels.blocks.BitItem
import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.blocks.BitStructureItem
import com.woalk.mods.bitsandchisels.blocks.BitsBlockItem
import com.woalk.mods.bitsandchisels.chisel.*
import com.woalk.mods.bitsandchisels.chisel.WrenchItem
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

@Suppress("unused")
object ModItems: ModRegister<Item, Item>(BuiltInRegistries.ITEM) {
    private fun itemSettings() = Item.Properties()

    //region item declarations
    @JvmField
    val BIT_ITEM = registerTyped("bit_item", { BitItem(it) }, Item.Properties())
    @JvmField
    val DIAMOND_CHISEL = registerTyped("diamond_chisel", { DiamondChisel(it) }, itemSettings())
    @JvmField
    val ENDER_CHISEL = registerTyped("ender_chisel", { EnderChisel(it) }, itemSettings())
    @JvmField
    val IRON_CHISEL = registerTyped("iron_chisel", { IronChisel(it) }, itemSettings())
    @JvmField
    val SMART_CHISEL = registerTyped("smart_chisel", { SmartChisel(it) }, itemSettings())
    @JvmField
    val WRENCH_ITEM = registerTyped("wrench", { WrenchItem(it) }, itemSettings())
    @JvmField
    val BITS_BLOCK_ITEM = registerTyped(
        "bits_block",
        { BitsBlockItem(BitsAndChisels.BITS_BLOCK, it) },
        Item.Properties().setId(BitsAndChisels.BITS_BLOCK_ITEM_ID.item),
        false,
    )

    val animationWand = registerTyped("animation_wand", { AnimationWand(it) }, itemSettings())
    val bitStructureItem = registerTyped("bit_structure", { BitStructureItem(it) }, Item.Properties().stacksTo(16))
    val duplicationKit = registerTyped("duplication_kit", { DuplicationKit(it) }, itemSettings().stacksTo(16))
    //endregion

    private val creativeModeTabKey = ResourceKey.create(
        BuiltInRegistries.CREATIVE_MODE_TAB.key(),
        Identifier.fromNamespaceAndPath(BitsAndChisels.MODID, "tab"),
    )
    val creativeModeTab = FabricCreativeModeTab.builder()
        .icon { ItemStack(IRON_CHISEL) }
        .title(Component.translatable("tab.bitsandchisels"))
        .displayItems { parameters, output ->
            output.accept(IRON_CHISEL)
            output.accept(DIAMOND_CHISEL)
            output.accept(SMART_CHISEL)
            output.accept(ENDER_CHISEL)
            output.accept(WRENCH_ITEM)
            output.accept(animationWand)
            output.accept(duplicationKit)
//            output.accept(ModBlocks.barrierBlock)
        }
        .build()

    //region convenience functions
    @Suppress("UNCHECKED_CAST")
    fun <T : Item> registerTyped(
        name: String,
        itemCreator: (Item.Properties) -> T,
        itemSettings: Item.Properties = itemSettings(),
        setId: Boolean = true
    ): T = register(name, itemCreator(itemSettings.apply {
        if (setId) {
            setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(BitsAndChisels.MODID, name)))
        }
    })) as T

    override fun onRegister(toRegister: Item): Item = toRegister

    override fun registerAll() {
        super.registerAll()

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, creativeModeTabKey, creativeModeTab)
    }
    //endregion
}