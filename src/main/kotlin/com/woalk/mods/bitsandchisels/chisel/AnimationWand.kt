package com.woalk.mods.bitsandchisels.chisel

import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.blocks.AnimatedBitsBlockEntity
import com.woalk.mods.bitsandchisels.registry.ModBlockEntities
import com.woalk.mods.bitsandchisels.registry.ModBlocks
import com.woalk.mods.bitsandchisels.registry.ModItems
import com.woalk.mods.bitsandchisels.registry.ModPermissions
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.item.TieredItem
import net.minecraft.world.item.Tiers
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.world.level.Level

class AnimationWand(settings: Properties) : TieredItem(Tiers.STONE, settings) {
    override fun useOn(context: UseOnContext?): InteractionResult {
        if (context?.player == null ||
            !Permissions.check(context.player!!, ModPermissions.USE_ANIMATION_WAND, true) ||
            !context.level.mayInteract(context.player!!, context.clickedPos)) {
            return InteractionResult.PASS
        }
        val be = context.level?.getBlockEntity(
            context.clickedPos
        ) ?: return InteractionResult.PASS

        if (be is AnimatedBitsBlockEntity) {
            if (context.level.isClientSide) return InteractionResult.sidedSuccess(true)
            if (context.player!!.isShiftKeyDown) {
                be.frametime = (be.frametime - 1).coerceAtLeast(1)
            } else be.frametime++
            be.sync()
            context.player!!.displayClientMessage(Component.literal("Set frame time to ${be.frametime}"), true)
            return InteractionResult.SUCCESS
        }

        val chestEntity = be as? ChestBlockEntity ?: return InteractionResult.PASS
        if (context.level.isClientSide) return InteractionResult.sidedSuccess(true)

        var error = false
        val frames = ListTag()
        for (slot in 0 until 27) {
            val stack = chestEntity.getItem(slot)
            if (stack.isEmpty) {
                continue
            } else if (!stack.`is`(ModItems.BITS_BLOCK_ITEM)) {
                error = true
                break
            } else {
                val tag = stack.components.get(DataComponents.BLOCK_ENTITY_DATA)?.copyTag()
                println(tag?.asString)
                frames.add(tag ?: continue)
            }
        }

        return if (error || frames.isEmpty()) {
            context.player?.sendSystemMessage(Component.literal("This chest contains items that are not bit blocks."))
            InteractionResult.FAIL
        } else {
            context.level.removeBlockEntity(context.clickedPos)
            val state = ModBlocks.animatedBitsBlock.defaultBlockState()
            context.level.setBlockAndUpdate(context.clickedPos, state)
            val be = context.level.getBlockEntity(context.clickedPos, ModBlockEntities.animatedBitsBlockEntity)
            if (be.isPresent) be.get().load(CompoundTag().apply {
                put(AnimatedBitsBlockEntity.FRAMES_KEY, frames)
            })
            InteractionResult.SUCCESS
        }
    }

    override fun appendHoverText(
        stack: ItemStack,
        tooltipContext: TooltipContext,
        list: MutableList<Component?>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, tooltipContext, list, tooltipFlag)
        list.add(
            Component.translatable("$descriptionId.desc")
                .withStyle(ChatFormatting.DARK_PURPLE)
                .withStyle(ChatFormatting.ITALIC)
        )
    }
}
