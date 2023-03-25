package com.woalk.mods.bitsandchisels

import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder
import net.minecraft.block.Blocks
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier

object WoalkItemGroups {
    private val WOALK_ITEM_GROUP_ID = Identifier(WoalkItemsMod.MOD_ID, "woalk")

    val WOALK_ITEMS by lazy {
        return@lazy getItemGroupByName(WOALK_ITEM_GROUP_ID)
            ?: FabricItemGroupBuilder.build(WOALK_ITEM_GROUP_ID) {
                ItemStack(Blocks.WHITE_CONCRETE)
            }
    }

    private fun getItemGroupByName(id: Identifier) =
        ItemGroup.GROUPS.find { it.name == "${id.namespace}.${id.path}" }
}