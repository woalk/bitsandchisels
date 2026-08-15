package com.woalk.mods.bitsandchisels.registry

import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.BitsBlock
import com.woalk.mods.bitsandchisels.WoalkItemsMod
import com.woalk.mods.bitsandchisels.blocks.*
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.world.level.block.*
import net.minecraft.client.color.block.BlockColor
import net.minecraft.client.color.item.ItemColor
import net.minecraft.client.renderer.RenderType
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.DyeableLeatherItem
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Rarity
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.state.BlockState

@Suppress("unused")
object ModBlocks : ModRegister<Pair<Block, (() -> FabricItemSettings)?>, Block>(BuiltInRegistries.BLOCK) {
    //region block declarations
    val animatedBitsBlock = register("animated_bits_block", Pair(AnimatedBitsBlock(
        FabricBlockSettings.of()
            .noOcclusion()
            .dynamicShape()
            .destroyTime(4.0f)
            .lightLevel { s -> s.getValue(BitsBlock.LIGHT_LEVEL) }
    )) { FabricItemSettings().maxCount(16) }).first

    val coloredBlock = register("coloured_block", ColoredBlock(
        FabricBlockSettings.copyOf(Blocks.WHITE_CONCRETE)
    ))
    val barrierBlock = register("barrier", Pair(
        Block(
        FabricBlockSettings.of()
            .noOcclusion()
            .destroyTime(0.3f)
    ), ::FabricItemSettings)).first
    //endregion

    //region convenience methods
    private val itemSettings by lazy {
        FabricItemSettings()
            .rarity(Rarity.EPIC)
    }

    private fun register(id: String, block: Block): Block =
        register(id, Pair(block) { itemSettings }).first

    override fun onRegister(toRegister: Pair<Block, (() -> FabricItemSettings)?>) = toRegister.first

    override fun getNamespace(id: String) = if (id == "animated_bits_block") BitsAndChisels.MODID else WoalkItemsMod.MOD_ID

    override fun registerNow(id: String, toRegister: Pair<Block, (() -> FabricItemSettings)?>) {
        super.registerNow(id, toRegister)
        if (toRegister.second != null) {
            Registry.register(
                BuiltInRegistries.ITEM,
                ResourceLocation(getNamespace(id), id),
                when (id) {
                    "coloured_block" -> ColoredBlockItem(toRegister.first, toRegister.second!!.invoke())
                    "animated_bits_block" -> BitsBlockItem(toRegister.first, toRegister.second!!.invoke())
                    else -> BlockItem(toRegister.first, toRegister.second!!.invoke())
                }
            )
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun never(state: BlockState, world: BlockGetter, pos: BlockPos, type: EntityType<*>? = null) = false

    @Environment(EnvType.CLIENT)
    fun registerAllClient() {
        ColorProviderRegistry.BLOCK.register(BlockColor { _, world, pos, _ ->
            (world?.getBlockEntity(pos) as? ColoredBlockEntity)?.colorWithAlpha ?: 0xFFFFFFFFU.toInt()
        }, coloredBlock)
        ColorProviderRegistry.ITEM.register(ItemColor { stack, _ ->
            stack.tag?.getCompound(DyeableLeatherItem.TAG_DISPLAY)?.getInt(DyeableLeatherItem.TAG_COLOR) ?: 0xFFFFFF
        }, coloredBlock.asItem())

        BlockRenderLayerMap.INSTANCE.putBlock(barrierBlock, RenderType.translucent())
    }
    //endregion
}