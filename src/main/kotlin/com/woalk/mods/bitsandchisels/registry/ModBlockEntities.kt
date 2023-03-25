package com.woalk.mods.bitsandchisels.registry

import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.blocks.ColoredBlockEntity
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

object ModBlockEntities : IModRegister {
    lateinit var coloredBlockEntity: BlockEntityType<ColoredBlockEntity>

    override fun registerAll() {
        coloredBlockEntity = Registry.register(
            Registry.BLOCK_ENTITY_TYPE,
            Identifier(BitsAndChisels.MODID, "coloured_block"),
            FabricBlockEntityTypeBuilder.create(
                ::ColoredBlockEntity,
                ModBlocks.coloredBlock
            ).build()
        )
    }
}
