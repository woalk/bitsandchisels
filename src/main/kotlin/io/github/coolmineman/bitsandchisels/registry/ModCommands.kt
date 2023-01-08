package io.github.coolmineman.bitsandchisels.registry

import com.mojang.brigadier.Command.SINGLE_SUCCESS
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.woalk.mods.roleplay.registry.IModRegister
import io.github.coolmineman.bitsandchisels.api.BitUtils
import io.github.coolmineman.bitsandchisels.image.ChiseledImage
import io.github.coolmineman.bitsandchisels.image.PaletteIdentifier
import io.github.coolmineman.bitsandchisels.image.canPlace
import io.github.coolmineman.bitsandchisels.image.place
import me.lucko.fabric.api.permissions.v0.Permissions
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
import net.minecraft.util.math.BlockPos

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

    private fun image2chisel(
        ctx: CommandContext<ServerCommandSource>,
        image: String,
        zMin: Int = 0,
        zMax: Int = 15,
        palette: PaletteIdentifier = PaletteIdentifier()
    ): Int {
        if (zMin !in 0 until 16 || zMax !in 0 until 16) {
            ctx.source.sendFeedback(
                Text.literal("Z not in range").formatted(Formatting.RED),
                false
            )
            return -1
        }
        try {
            val chiseledImage = ChiseledImage(image)

            ctx.source.sendFeedback(Text.literal("image2chisel: Loaded $image"), true)

            val blockPos = BlockPos(ctx.source.position)
            if (!chiseledImage.canPlace(ctx.source.playerOrThrow, blockPos)) {
                throw IllegalArgumentException("You cannot modify blocks in the target area")
            }
            chiseledImage.place(ctx.source.world, blockPos, zMin..zMax, palette)

            ctx.source.sendFeedback(Text.literal(
                "image2chisel: Placed $image at (${blockPos.toShortString()})"),
                true
            )
        } catch (e: Exception) {
            ctx.source.sendFeedback(
                Text.literal(e.message).formatted(Formatting.RED),
                false
            )
            return -2
        }

        return SINGLE_SUCCESS
    }

    override fun registerAll() {
        CommandRegistrationCallback.EVENT.register { dispatcher, commandRegistryAccess, _ ->
            dispatcher.register(
                literal("bit")
                    .requires(Permissions.require(ModPermissions.COMMAND_BIT, 2))
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
            dispatcher.register(
                literal("image2chisel")
                    .requires(Permissions.require(ModPermissions.COMMAND_IMAGE2CHISEL, 2))
                    .then(
                        argument("image", StringArgumentType.string())
                            .executes { ctx ->
                                return@executes image2chisel(
                                    ctx,
                                    StringArgumentType.getString(ctx, "image")
                                )
                            }
                    )
            )
        }
    }
}