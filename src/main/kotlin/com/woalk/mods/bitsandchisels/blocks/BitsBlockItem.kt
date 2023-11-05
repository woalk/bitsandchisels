package com.woalk.mods.bitsandchisels.blocks

import net.minecraft.block.Block
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtElement
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.world.World

class BitsBlockItem(block: Block, settings: Settings) : BlockItem(block, settings) {
    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: TooltipContext
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        if (context.isAdvanced) {
            stack.getSubNbt("BlockStateTag")?.getInt("shader_type")?.let {
                tooltip.add(Text.translatable(
                    "gui.bitsandchisels.shader_type.tooltip",
                    Text.translatable("gui.bitsandchisels.shader_type.$it")
                        .formatted(Formatting.GOLD, Formatting.ITALIC)
                ).formatted(Formatting.GRAY))
            }
            stack.getSubNbt("BlockEntityTag")?.getList("palette", NbtElement.COMPOUND_TYPE.toInt())?.let {
                if (it.isNotEmpty()) {
                    tooltip.add(
                        Text.translatable("gui.bitsandchisels.palette.tooltip", it.size)
                            .formatted(Formatting.GRAY)
                    )
                }
            }
            stack.getSubNbt("BlockEntityTag")?.getList("frames", NbtElement.COMPOUND_TYPE.toInt())?.let {
                if (it.isNotEmpty()) {
                    tooltip.add(
                        Text.translatable("gui.bitsandchisels.frames.tooltip", it.size)
                            .formatted(Formatting.GRAY)
                    )
                }
            }
        }
    }
}