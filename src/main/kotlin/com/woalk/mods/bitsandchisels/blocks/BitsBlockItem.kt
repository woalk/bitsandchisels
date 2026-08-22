package com.woalk.mods.bitsandchisels.blocks

import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.BitsBlock
import com.woalk.mods.bitsandchisels.blocks.BitStructureItem.Companion.NBT_ROTATION
import com.woalk.mods.bitsandchisels.utils.toYaw
import net.minecraft.world.level.block.Block
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.item.component.TypedEntityData
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.ChatFormatting
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import kotlin.math.roundToInt
import java.util.function.Consumer

class BitsBlockItem(block: Block, settings: Properties) : BlockItem(block, settings) {
    override fun place(context: BlockPlaceContext): InteractionResult {
        val stack = context.itemInHand
        val blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA)
        val nbt: CompoundTag? = blockEntityData?.getUnsafe()?.copy()
        if (nbt != null && !context.level.isClientSide && nbt.contains(NBT_ROTATION)) {
            val rotation =
                ((Direction.from3DDataValue(nbt.getIntOr(NBT_ROTATION, 0)).getUnitVec3i().toYaw() - context.rotation) / 90).roundToInt()
            // TODO: WorldEdit rotation support needs updating for 26.2
            // ChiselExtent rotation was removed as NBTConverter API changed
        }
        val newContext = if (nbt != null) {
            val newStack = stack.copy()
            newStack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(BitsAndChisels.BITS_BLOCK_ENTITY, nbt))
            BlockPlaceContext(context.level, context.player, context.hand, newStack, context.hitResult)
        } else {
            context
        }
        return super.place(newContext)
    }

    override fun appendHoverText(
        itemStack: ItemStack,
        context: TooltipContext,
        display: TooltipDisplay,
        builder: Consumer<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag)
        if (tooltipFlag.isAdvanced) {
            itemStack.components.get(DataComponents.BLOCK_STATE)?.get(BitsBlock.SHADER_TYPE)?.let {
                builder.accept(
                    Component.translatable(
                    "gui.bitsandchisels.shader_type.tooltip",
                    Component.translatable("gui.bitsandchisels.shader_type.$it")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC)
                ).withStyle(ChatFormatting.GRAY))
            }
            val blockEntityData = itemStack.components.get(DataComponents.BLOCK_ENTITY_DATA)?.getUnsafe()
            blockEntityData?.getList("palette")?.orElse(null)?.let {
                if (it.isNotEmpty()) {
                    builder.accept(
                        Component.translatable("gui.bitsandchisels.palette.tooltip", it.size)
                            .withStyle(ChatFormatting.GRAY)
                    )
                }
            }
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
}
