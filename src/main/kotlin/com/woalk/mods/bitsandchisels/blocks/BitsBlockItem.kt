package com.woalk.mods.bitsandchisels.blocks

import com.sk89q.worldedit.fabric.internal.NBTConverter
import com.sk89q.worldedit.math.transform.AffineTransform
import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.BitsBlock
import com.woalk.mods.bitsandchisels.blocks.BitStructureItem.Companion.NBT_ROTATION
import com.woalk.mods.bitsandchisels.compat.ChiselExtent
import com.woalk.mods.bitsandchisels.utils.toYaw
import net.minecraft.world.level.block.Block
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.ChatFormatting
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import kotlin.math.roundToInt

class BitsBlockItem(block: Block, settings: Properties) : BlockItem(block, settings) {
    override fun place(context: BlockPlaceContext): InteractionResult {
        val stack = context.itemInHand
        val blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA)
        val nbt = blockEntityData?.copyTag()?.apply {
            if (!context.level.isClientSide && this.contains(NBT_ROTATION)) {
                val rotation =
                    ((Direction.from3DDataValue(this.getInt(NBT_ROTATION)).normal.toYaw() - context.rotation) / 90).roundToInt()
                val weConvertedNbt = NBTConverter.fromNative(this)
                val rotatedNbt = NBTConverter.toNative(
                    ChiselExtent.apply(AffineTransform().rotateY(rotation * 90.0), weConvertedNbt)
                )
                this.put("bits_v2", rotatedNbt.get("bits_v2"))
                this.put("palette", rotatedNbt.get("palette"))
            }
        }
        val newContext = if (nbt != null) {
            val newStack = stack.copy()
            setBlockEntityData(newStack, BitsAndChisels.BITS_BLOCK_ENTITY, nbt)
            BlockPlaceContext(context.level, context.player, context.hand, newStack, context.hitResult)
        } else {
            context
        }
        return super.place(newContext)
    }

    override fun appendHoverText(
        itemStack: ItemStack,
        tooltipContext: TooltipContext,
        list: MutableList<Component?>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(itemStack, tooltipContext, list, tooltipFlag)
        if (tooltipFlag.isAdvanced) {
            itemStack.components.get(DataComponents.BLOCK_STATE)?.get(BitsBlock.SHADER_TYPE)?.let {
                list.add(
                    Component.translatable(
                    "gui.bitsandchisels.shader_type.tooltip",
                    Component.translatable("gui.bitsandchisels.shader_type.$it")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC)
                ).withStyle(ChatFormatting.GRAY))
            }
            val blockEntityData = itemStack.components.get(DataComponents.BLOCK_ENTITY_DATA)?.copyTag()
            blockEntityData?.getList("palette", Tag.TAG_COMPOUND.toInt())?.let {
                if (it.isNotEmpty()) {
                    list.add(
                        Component.translatable("gui.bitsandchisels.palette.tooltip", it.size)
                            .withStyle(ChatFormatting.GRAY)
                    )
                }
            }
            blockEntityData?.getList("frames", Tag.TAG_COMPOUND.toInt())?.let {
                if (it.isNotEmpty()) {
                    list.add(
                        Component.translatable("gui.bitsandchisels.frames.tooltip", it.size)
                            .withStyle(ChatFormatting.GRAY)
                    )
                }
            }
        }
    }
}