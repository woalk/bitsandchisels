package com.woalk.mods.bitsandchisels.registry

import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.blocks.AnimatedBitsBlockEntity
import com.woalk.mods.bitsandchisels.blocks.ColoredBlockEntity
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries

object ModBlockEntities : IModRegister {
    lateinit var animatedBitsBlockEntity: BlockEntityType<AnimatedBitsBlockEntity>
    lateinit var coloredBlockEntity: BlockEntityType<ColoredBlockEntity>

    override fun registerAll() {
        animatedBitsBlockEntity = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation(BitsAndChisels.MODID, "animated_bits_block"),
            FabricBlockEntityTypeBuilder.create(
                ::AnimatedBitsBlockEntity,
                ModBlocks.animatedBitsBlock,
            ).build()
        )
        coloredBlockEntity = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation(BitsAndChisels.MODID, "coloured_block"),
            FabricBlockEntityTypeBuilder.create(
                ::ColoredBlockEntity,
                ModBlocks.coloredBlock
            ).build()
        )
    }
}
