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

object ModCommands : IModRegister {
    private fun giveBit(
        ctx: CommandContext<ServerCommandSource>,
        player: ServerPlayerEntity?,
        block: BlockState?
    ): Int {
        if (player == null || block == null) {
            return -1
        }
        player.giveItemStack(BitUtils.getBitItemStack(block))
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