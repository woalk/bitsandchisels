package com.woalk.mods.bitsandchisels.registry

import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.BitsBlock
import com.woalk.mods.bitsandchisels.WoalkItemGroups
import com.woalk.mods.bitsandchisels.WoalkItemsMod
import com.woalk.mods.bitsandchisels.blocks.*
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.*
import net.minecraft.client.color.block.BlockColorProvider
import net.minecraft.client.color.item.ItemColorProvider
import net.minecraft.client.render.RenderLayer
import net.minecraft.entity.EntityType
import net.minecraft.item.BlockItem
import net.minecraft.item.DyeableItem
import net.minecraft.util.Identifier
import net.minecraft.util.Rarity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import net.minecraft.world.BlockView

@Suppress("unused")
object ModBlocks : ModRegister<Pair<Block, (() -> FabricItemSettings)?>, Block>(Registry.BLOCK) {
    //region block declarations
    val animatedBitsBlock = register("animated_bits_block", Pair(AnimatedBitsBlock(
        FabricBlockSettings.of(Material.METAL)
            .nonOpaque()
            .dynamicBounds()
            .hardness(4.0f)
            .luminance { s -> s.get(BitsBlock.LIGHT_LEVEL) }
    )) { FabricItemSettings().maxCount(16) }).first

    val coloredBlock = register("coloured_block", ColoredBlock(
        FabricBlockSettings.copy(Blocks.WHITE_CONCRETE)
    ))
    val barrierBlock = register("barrier", Pair(Block(
        FabricBlockSettings.of(Material.BARRIER)
            .nonOpaque()
            .hardness(0.3f)
    ), ::FabricItemSettings)).first
    //endregion

    //region convenience methods
    private val itemSettings by lazy {
        FabricItemSettings()
            .rarity(Rarity.EPIC)
            .apply {
                if (WoalkItemsMod.isAdmin) {
                    group(WoalkItemGroups.WOALK_ITEMS)
                }
            }
    }

    private fun register(id: String, block: Block): Block =
        register(id, Pair(block) { itemSettings }).first

    override fun onRegister(toRegister: Pair<Block, (() -> FabricItemSettings)?>) = toRegister.first

    override fun getNamespace(id: String) = if (id == "animated_bits_block") BitsAndChisels.MODID else WoalkItemsMod.MOD_ID

    override fun registerNow(id: String, toRegister: Pair<Block, (() -> FabricItemSettings)?>) {
        super.registerNow(id, toRegister)
        if (toRegister.second != null) {
            Registry.register(
                Registry.ITEM,
                Identifier(getNamespace(id), id),
                when (id) {
                    "coloured_block" -> ColoredBlockItem(toRegister.first, toRegister.second!!.invoke())
                    "animated_bits_block" -> BitsBlockItem(toRegister.first, toRegister.second!!.invoke())
                    else -> BlockItem(toRegister.first, toRegister.second!!.invoke())
                }
            )
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun never(state: BlockState, world: BlockView, pos: BlockPos, type: EntityType<*>? = null) = false

    @Environment(EnvType.CLIENT)
    fun registerAllClient() {
        ColorProviderRegistry.BLOCK.register(BlockColorProvider { _, world, pos, _ ->
            (world?.getBlockEntity(pos) as? ColoredBlockEntity)?.colorWithAlpha ?: 0xFFFFFFFFU.toInt()
        }, coloredBlock)
        ColorProviderRegistry.ITEM.register(ItemColorProvider { stack, _ ->
            stack.nbt?.getCompound(DyeableItem.DISPLAY_KEY)?.getInt(DyeableItem.COLOR_KEY) ?: 0xFFFFFF
        }, coloredBlock.asItem())

        BlockRenderLayerMap.INSTANCE.putBlock(barrierBlock, RenderLayer.getTranslucent())
    }
    //endregion
}