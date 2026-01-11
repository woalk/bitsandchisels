package com.woalk.mods.bitsandchisels.blocks

import com.sk89q.worldedit.fabric.internal.NBTConverter
import com.sk89q.worldedit.math.transform.AffineTransform
import com.woalk.mods.bitsandchisels.blocks.BitStructureItem.Companion.NBT_ROTATION
import com.woalk.mods.bitsandchisels.compat.ChiselExtent
import com.woalk.mods.bitsandchisels.utils.toYaw
import net.minecraft.block.Block
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtElement
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import kotlin.math.roundToInt

class BitsBlockItem(block: Block, settings: Settings) : BlockItem(block, settings) {
    override fun place(context: ItemPlacementContext): ActionResult? {
        val nbt = getBlockEntityNbt(context.stack)?.copy()?.apply {
            if (!context.world.isClient && this.contains(NBT_ROTATION)) {
                val rotation =
                    ((Direction.byId(this.getInt(NBT_ROTATION)).vector.toYaw() - context.playerYaw) / 90).roundToInt()
                val weConvertedNbt = NBTConverter.fromNative(this)
                val rotatedNbt = NBTConverter.toNative(
                    ChiselExtent.apply(AffineTransform().rotateY(rotation * 90.0), weConvertedNbt)
                )
                this.put("bits_v2", rotatedNbt.get("bits_v2"))
                this.put("palette", rotatedNbt.get("palette"))
            }
        }
        val newContext = ItemPlacementContext(context.world, context.player, context.hand, context.stack.copy().apply {
            setSubNbt("BlockEntityTag", nbt)
        }, context.hitResult)
        return super.place(newContext)
    }

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