package com.woalk.mods.bitsandchisels.blocks

import com.sk89q.worldedit.fabric.internal.NBTConverter
import com.sk89q.worldedit.math.transform.AffineTransform
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
import net.minecraft.world.level.Level
import kotlin.math.roundToInt

class BitsBlockItem(block: Block, settings: Properties) : BlockItem(block, settings) {
    override fun place(context: BlockPlaceContext): InteractionResult? {
        val nbt = getBlockEntityData(context.itemInHand)?.copy()?.apply {
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
        val newContext = BlockPlaceContext(context.level, context.player, context.hand, context.itemInHand.copy().apply {
            addTagElement("BlockEntityTag", nbt)
        }, context.hitResult)
        return super.place(newContext)
    }

    override fun appendHoverText(
        stack: ItemStack,
        world: Level?,
        tooltip: MutableList<Component>,
        context: TooltipFlag
    ) {
        super.appendHoverText(stack, world, tooltip, context)
        if (context.isAdvanced) {
            stack.getTagElement("BlockStateTag")?.getInt("shader_type")?.let {
                tooltip.add(
                    Component.translatable(
                    "gui.bitsandchisels.shader_type.tooltip",
                    Component.translatable("gui.bitsandchisels.shader_type.$it")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC)
                ).withStyle(ChatFormatting.GRAY))
            }
            stack.getTagElement("BlockEntityTag")?.getList("palette", Tag.TAG_COMPOUND.toInt())?.let {
                if (it.isNotEmpty()) {
                    tooltip.add(
                        Component.translatable("gui.bitsandchisels.palette.tooltip", it.size)
                            .withStyle(ChatFormatting.GRAY)
                    )
                }
            }
            stack.getTagElement("BlockEntityTag")?.getList("frames", Tag.TAG_COMPOUND.toInt())?.let {
                if (it.isNotEmpty()) {
                    tooltip.add(
                        Component.translatable("gui.bitsandchisels.frames.tooltip", it.size)
                            .withStyle(ChatFormatting.GRAY)
                    )
                }
            }
        }
    }
}