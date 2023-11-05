package com.woalk.mods.bitsandchisels.chisel

import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.blocks.AnimatedBitsBlockEntity
import com.woalk.mods.bitsandchisels.registry.ModBlockEntities
import com.woalk.mods.bitsandchisels.registry.ModBlocks
import com.woalk.mods.bitsandchisels.registry.ModPermissions
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.ToolItem
import net.minecraft.item.ToolMaterials
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.world.World

class AnimationWand(settings: Settings) : ToolItem(ToolMaterials.STONE, settings) {
    override fun useOnBlock(context: ItemUsageContext?): ActionResult {
        if (context?.player == null ||
            !Permissions.check(context.player!!, ModPermissions.USE_ANIMATION_WAND, true) ||
            !context.world.canPlayerModifyAt(context.player!!, context.blockPos)) {
            return ActionResult.PASS
        }
        val be = context.world?.getBlockEntity(
            context.blockPos
        ) ?: return ActionResult.PASS

        if (be is AnimatedBitsBlockEntity) {
            if (context.world.isClient) return ActionResult.success(true)
            if (context.player!!.isSneaking) {
                be.frametime = (be.frametime - 1).coerceAtLeast(1)
            } else be.frametime++
            be.sync()
            context.player!!.sendMessage(Text.literal("Set frame time to ${be.frametime}"), true)
            return ActionResult.SUCCESS
        }

        val chestEntity = be as? ChestBlockEntity ?: return ActionResult.PASS
        if (context.world.isClient) return ActionResult.success(true)

        var error = false
        val frames = NbtList()
        for (slot in 0 until 27) {
            val stack = chestEntity.getStack(slot)
            if (stack.isEmpty) {
                continue
            } else if (!stack.isOf(BitsAndChisels.BITS_BLOCK_ITEM)) {
                error = true
                break
            } else {
                val tag = stack.getSubNbt("BlockEntityTag")
                println(tag?.asString())
                frames.add(tag ?: continue)
            }
        }

        return if (error || frames.isEmpty()) {
            context.player?.sendMessage(Text.literal("This chest contains items that are not bit blocks."))
            ActionResult.FAIL
        } else {
            context.world.removeBlockEntity(context.blockPos)
            val state = ModBlocks.animatedBitsBlock.defaultState
            context.world.setBlockState(context.blockPos, state)
            val be = context.world.getBlockEntity(context.blockPos, ModBlockEntities.animatedBitsBlockEntity)
            if (be.isPresent) be.get().readNbt(NbtCompound().apply {
                put(AnimatedBitsBlockEntity.FRAMES_KEY, frames)
            })
            ActionResult.SUCCESS
        }
    }

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: TooltipContext?
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        tooltip.add(
            Text.translatable("$translationKey.desc")
                .formatted(Formatting.DARK_PURPLE)
                .formatted(Formatting.ITALIC)
        )
    }
}
