package com.woalk.mods.bitsandchisels

import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraft.resources.ResourceLocation

object WoalkItemGroups {
    private val WOALK_ITEM_GROUP_ID = ResourceLocation(WoalkItemsMod.MOD_ID, "woalk")

    val WOALK_ITEMS by lazy {
        return@lazy getItemGroupByName(WOALK_ITEM_GROUP_ID)
            ?: FabricItemGroupBuilder.build(WOALK_ITEM_GROUP_ID) {
                ItemStack(Blocks.WHITE_CONCRETE)
            }
    }

    private fun getItemGroupByName(id: ResourceLocation) =
        CreativeModeTab.TABS.find { it.recipeFolderName == "${id.namespace}.${id.path}" }
}