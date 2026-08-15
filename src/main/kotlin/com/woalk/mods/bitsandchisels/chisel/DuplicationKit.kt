package com.woalk.mods.bitsandchisels.chisel

import com.woalk.mods.bitsandchisels.BitsBlock
import com.woalk.mods.bitsandchisels.registry.ModPermissions
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUtils
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.nbt.Tag
import net.minecraft.nbt.StringTag
import net.minecraft.sounds.SoundEvents
import net.minecraft.stats.Stats
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.ChatFormatting
import net.minecraft.world.level.Level

class DuplicationKit(settings: Properties) : Item(settings) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val blockState = context.level?.getBlockState(context.clickedPos)
        val block = blockState?.block
        if (block is BitsBlock) {
            if (context.level.isClientSide) {
                context.player?.apply {
                    playSound(SoundEvents.ANVIL_USE, 1.0f, 1.7f)
                    playSound(SoundEvents.ANVIL_USE, 1.0f, 0.7f + context.level.random.nextFloat() * 0.05f)
                }
                return InteractionResult.SUCCESS
            }
            if (context.player == null ||
                !Permissions.check(context.player!!, ModPermissions.USE_DUPLICATION_KIT, true) ||
                !context.level.mayInteract(context.player!!, context.clickedPos)
            ) {
                return InteractionResult.PASS
            }
            val itemStack = block.getDropItem(context.level, context.clickedPos, blockState, null, context.player, false)
            if (itemStack != null) {
                itemStack.getTagElement("display")?.apply {
                    val lore = getList("Lore", Tag.TAG_STRING.toInt())
                    lore.add(
                        StringTag.valueOf(
                            Component.Serializer.toJson(
                                Component.translatable("$descriptionId.remark", context.player!!.displayName)
                                    .withStyle(ChatFormatting.GRAY)
                            )
                        )
                    )
                    put("Lore", lore)
                }
                println("Duplicated item $itemStack ${itemStack.tag.toString()}")
                context.player!!.awardStat(Stats.ITEM_USED.get(this))
                val newStack = ItemUtils.createFilledResult(context.itemInHand, context.player, itemStack, false)
                context.player!!.setItemInHand(context.hand, newStack)
                return InteractionResult.SUCCESS
            }
        }
        return InteractionResult.FAIL
    }

    override fun appendHoverText(stack: ItemStack?, world: Level?, tooltip: MutableList<Component?>, context: TooltipFlag) {
        super.appendHoverText(stack, world, tooltip, context)
        tooltip.add(
            Component.translatable("$descriptionId.desc1").withStyle(ChatFormatting.DARK_PURPLE).withStyle(
                ChatFormatting.ITALIC))
        tooltip.add(
            Component.translatable("$descriptionId.desc2").withStyle(ChatFormatting.DARK_PURPLE).withStyle(
                ChatFormatting.ITALIC))
    }
}