package com.woalk.mods.bitsandchisels.registry

import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.BitsBlock
import com.woalk.mods.bitsandchisels.WoalkItemsMod
import com.woalk.mods.bitsandchisels.blocks.*
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.world.level.block.*
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.BlockItem
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Rarity
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.references.BlockItemId
import net.minecraft.world.item.Item
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState

@Suppress("unused")
object ModBlocks : ModRegister<Pair<Block, (() -> Item.Properties)?>, Block>(BuiltInRegistries.BLOCK) {
    //region block declarations
    val animatedBitsBlock = register("animated_bits_block", { AnimatedBitsBlock(it) },
        BlockBehaviour.Properties.of()
            .noOcclusion()
            .dynamicShape()
            .destroyTime(4.0f)
            .lightLevel { s -> s.getValue(BitsBlock.LIGHT_LEVEL) }
    ) { Item.Properties().stacksTo(16) }

    val coloredBlock = register("coloured_block", { ColoredBlock(it) },
        BlockBehaviour.Properties.ofFullCopy(Blocks.CONCRETE.white)
    )
    val barrierBlock = register("barrier",
        { Block(it) },
        BlockBehaviour.Properties.of()
            .noOcclusion()
            .destroyTime(0.3f)
    ) { Item.Properties() }
    //endregion

    //region convenience methods
    private val itemSettings by lazy {
        Item.Properties()
            .rarity(Rarity.EPIC)
    }

    private fun register(id: String, block: Block): Block =
        register(id, Pair(block) { itemSettings }).first

    private fun register(
        id: String,
        block: (BlockBehaviour.Properties) -> Block,
        blockProperties: BlockBehaviour.Properties,
        item: () -> Item.Properties = { itemSettings },
    ): Block {
        val identifier = Identifier.fromNamespaceAndPath(getNamespace(id), id)
        val blockItemId = BlockItemId.create(identifier, identifier)
        return register(
            id,
            Pair(block(blockProperties.setId(blockItemId.block))) { item().setId(blockItemId.item) }).first
    }

    override fun onRegister(toRegister: Pair<Block, (() -> Item.Properties)?>) = toRegister.first

    override fun getNamespace(id: String) = if (id == "animated_bits_block") BitsAndChisels.MODID else WoalkItemsMod.MOD_ID

    override fun registerNow(id: String, toRegister: Pair<Block, (() -> Item.Properties)?>) {
        super.registerNow(id, toRegister)
        if (toRegister.second != null) {
            Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(getNamespace(id), id),
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
    }
    //endregion
}