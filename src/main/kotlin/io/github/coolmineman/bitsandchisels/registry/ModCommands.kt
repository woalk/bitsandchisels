package io.github.coolmineman.bitsandchisels.registry

import com.mojang.brigadier.Command.SINGLE_SUCCESS
import com.mojang.brigadier.context.CommandContext
import com.woalk.mods.roleplay.registry.IModRegister
import io.github.coolmineman.bitsandchisels.api.BitUtils
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.block.BlockState
import net.minecraft.command.argument.BlockStateArgumentType
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.item.BlockItem
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object ModCommands : IModRegister {
    private fun giveBit(
        ctx: CommandContext<ServerCommandSource>,
        player: ServerPlayerEntity?,
        block: BlockState?,
        count: Int = 1
    ): Int {
        if (player == null) {
            ctx.source.sendFeedback(
                Text.literal("No target player given").formatted(Formatting.RED),
                false
            )
            return -1
        }
        if (block == null) {
            ctx.source.sendFeedback(
                Text.literal("No block given").formatted(Formatting.RED),
                false
            )
            return -2
        }
        val bit = BitUtils.getBitItemStack(block)
        bit.count = count
        player.giveItemStack(bit)
        ctx.source.sendFeedback(
            Text.literal("Gave $count ")
                .append(block.block.name)
                .append(Text.literal(" bit to "))
                .append(player.displayName),
            true
        )
        return SINGLE_SUCCESS
    }

    override fun registerAll() {
        CommandRegistrationCallback.EVENT.register { dispatcher, commandRegistryAccess, _ ->
            dispatcher.register(
                literal("bit")
                    .requires {
                        it.hasPermissionLevel(2)
                    }
                    .executes { ctx ->
                        return@executes giveBit(
                            ctx,
                            ctx.source.player,
                            (ctx.source.player?.mainHandStack?.item as? BlockItem)?.block?.defaultState
                        )
                    }
                    .then(
                        argument("player", EntityArgumentType.player())
                            .then(
                                argument("block", BlockStateArgumentType.blockState(commandRegistryAccess))
                                    .executes { ctx ->
                                        return@executes giveBit(
                                            ctx,
                                            ctx.source.player,
                                            BlockStateArgumentType.getBlockState(ctx, "block").blockState
                                        )
                                    }
                            )
                    )
                    .then(
                        argument("block", BlockStateArgumentType.blockState(commandRegistryAccess))
                            .executes { ctx ->
                                return@executes giveBit(
                                    ctx,
                                    ctx.source.player,
                                    BlockStateArgumentType.getBlockState(ctx, "block").blockState
                                )
                            }
                    )
            )
        }
    }
}