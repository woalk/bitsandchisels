package com.woalk.mods.bitsandchisels.chisel

import com.woalk.mods.bitsandchisels.BitsBlock
import com.woalk.mods.bitsandchisels.registry.ModPermissions
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsage
import net.minecraft.item.ItemUsageContext
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtString
import net.minecraft.sound.SoundEvents
import net.minecraft.stat.Stats
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.world.World

class DuplicationKit(settings: Settings) : Item(settings) {
    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val blockState = context.world?.getBlockState(context.blockPos)
        val block = blockState?.block
        if (block is BitsBlock) {
            if (context.world.isClient) {
                context.player?.apply {
                    playSound(SoundEvents.BLOCK_ANVIL_USE, 1.0f, 1.7f)
                    playSound(SoundEvents.BLOCK_ANVIL_USE, 1.0f, 0.7f + context.world.random.nextFloat() * 0.05f)
                }
                return ActionResult.SUCCESS
            }
            if (context.player == null ||
                !Permissions.check(context.player!!, ModPermissions.USE_DUPLICATION_KIT, true) ||
                !context.world.canPlayerModifyAt(context.player!!, context.blockPos)
            ) {
                return ActionResult.PASS
            }
            val itemStack = block.getDropItem(context.world, context.blockPos, blockState, null, context.player, false)
            if (itemStack != null) {
                itemStack.getSubNbt("display")?.apply {
                    val lore = getList("Lore", NbtElement.STRING_TYPE.toInt())
                    lore.add(
                        NbtString.of(
                            Text.Serializer.toJson(
                                Text.translatable("$translationKey.remark", context.player!!.displayName)
                                    .formatted(Formatting.GRAY)
                            )
                        )
                    )
                    put("Lore", lore)
                }
                println("Duplicated item $itemStack ${itemStack.nbt.toString()}")
                context.player!!.incrementStat(Stats.USED.getOrCreateStat(this))
                val newStack = ItemUsage.exchangeStack(context.stack, context.player, itemStack, false)
                context.player!!.setStackInHand(context.hand, newStack)
                return ActionResult.SUCCESS
            }
        }
        return ActionResult.FAIL
    }

    override fun appendTooltip(stack: ItemStack?, world: World?, tooltip: MutableList<Text?>, context: TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        tooltip.add(Text.translatable("$translationKey.desc1").formatted(Formatting.DARK_PURPLE).formatted(Formatting.ITALIC))
        tooltip.add(Text.translatable("$translationKey.desc2").formatted(Formatting.DARK_PURPLE).formatted(Formatting.ITALIC))
    }
}