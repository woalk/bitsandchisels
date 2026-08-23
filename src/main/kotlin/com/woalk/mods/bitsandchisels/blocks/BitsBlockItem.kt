package com.woalk.mods.bitsandchisels.blocks

import com.sk89q.worldedit.coremc.internal.NBTConverter
import com.sk89q.worldedit.math.transform.AffineTransform
import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.blocks.BitStructureItem.Companion.NBT_ROTATION
import com.woalk.mods.bitsandchisels.compat.ChiselExtent
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
import net.minecraft.world.level.block.entity.BlockEntityType
import kotlin.math.roundToInt
import java.util.function.Consumer

open class BitsBlockItem(block: Block, settings: Properties) : BlockItem(block, settings) {
    override fun place(context: BlockPlaceContext): InteractionResult {
        val stack = context.itemInHand
        val blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA)
        val nbt = blockEntityData?.copyTagWithoutId()?.apply {
            if (!context.level.isClientSide && this.contains(NBT_ROTATION)) {
                val rotation =
                    ((Direction.from3DDataValue(this.getIntOr(NBT_ROTATION, 0)).unitVec3i.toYaw() - context.rotation) / 90).roundToInt()
                val weConvertedNbt = NBTConverter.fromNative(this)
                val rotatedNbt = NBTConverter.toNative(
                    ChiselExtent.apply(AffineTransform().rotateY(rotation * 90.0), weConvertedNbt)
                )
                this.put("bits_v2", rotatedNbt.get("bits_v2")!!)
                this.put("palette", rotatedNbt.get("palette")!!)
            }
        }
        val newContext = if (nbt != null) {
            val newStack = stack.copy()
            newStack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(getBlockEntityType(), nbt))
            BlockPlaceContext(context.level, context.player, context.hand, newStack, context.hitResult)
        } else {
            context
        }
        return super.place(newContext)
    }

    protected open fun getBlockEntityType(): BlockEntityType<out BitsBlockEntity> = BitsAndChisels.BITS_BLOCK_ENTITY!!

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
            val blockEntityData = itemStack.components.get(DataComponents.BLOCK_ENTITY_DATA)?.copyTagWithoutId()
            blockEntityData?.getList("palette")?.orElse(null)?.let {
                if (it.isNotEmpty()) {
                    builder.accept(
                        Component.translatable("gui.bitsandchisels.palette.tooltip", it.size)
                            .withStyle(ChatFormatting.GRAY)
                    )
                }
            }
        }
    }
}
