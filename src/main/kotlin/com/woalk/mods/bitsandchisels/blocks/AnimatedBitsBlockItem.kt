package com.woalk.mods.bitsandchisels.blocks

import com.woalk.mods.bitsandchisels.registry.ModBlockEntities
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.level.block.Block
import java.util.function.Consumer

class AnimatedBitsBlockItem(block: Block, settings: Properties) : BitsBlockItem(block, settings) {
    override fun getBlockEntityType() = ModBlockEntities.animatedBitsBlockEntity

    override fun appendHoverText(
        itemStack: ItemStack,
        context: TooltipContext,
        display: TooltipDisplay,
        builder: Consumer<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag)
        val blockEntityData = itemStack.components.get(DataComponents.BLOCK_ENTITY_DATA)?.copyTagWithoutId()
        blockEntityData?.getList("frames")?.orElse(null)?.let {
            if (it.isNotEmpty()) {
                builder.accept(
                    Component.translatable("gui.bitsandchisels.frames.tooltip", it.size)
                        .withStyle(ChatFormatting.GRAY)
                )
            }
        }
    }
}