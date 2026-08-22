package com.woalk.mods.bitsandchisels.registry

import com.mojang.brigadier.Command.SINGLE_SUCCESS
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.woalk.mods.bitsandchisels.api.BitUtils
import com.woalk.mods.bitsandchisels.blocks.ColoredBlockState
import com.woalk.mods.bitsandchisels.image.*
import com.woalk.mods.bitsandchisels.utils.DirectionArgumentType
import com.woalk.mods.bitsandchisels.utils.toColor
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.commands.arguments.blocks.BlockStateArgument
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.world.item.BlockItem
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

object ModCommands : IModRegister {
    private fun giveBit(
        ctx: CommandContext<CommandSourceStack>,
        player: ServerPlayer?,
        block: BlockState?,
        count: Int = 1
    ): Int {
        if (player == null) {
            ctx.source.sendSuccess({
                Component.literal("No target player given").withStyle(ChatFormatting.RED)
            }, false)
            return -1
        }
        if (block == null) {
            ctx.source.sendSuccess({
                Component.literal("No block given").withStyle(ChatFormatting.RED)
            }, false)
            return -2
        }
        val bit = BitUtils.getBitItemStack(block)
        bit.count = count
        player.addItem(bit)
        ctx.source.sendSuccess({
            Component.literal("Gave $count ")
                .append(block.block.name)
                .append(Component.literal(" bit to "))
                .append(player.displayName)
        }, true)
        return SINGLE_SUCCESS
    }

    private fun giveColoredBlockBit(
        ctx: CommandContext<CommandSourceStack>,
        player: ServerPlayer? = ctx.source.player,
        colorString: String,
        count: Int = 1
    ): Int {
        if (player == null) {
            ctx.source.sendSuccess({
                Component.literal("No target player given").withStyle(ChatFormatting.RED)
            }, false)
            return -1
        }
        val color = colorString.toColor()
        if (color == null || color <= -1) {
            ctx.source.sendSuccess({
                Component.literal("Invalid color").withStyle(ChatFormatting.RED)
            }, false)
            return -2
        }
        val bit = BitUtils.getBitItemStack(ColoredBlockState(color))
        bit.count = count
        player.addItem(bit)
        ctx.source.sendSuccess({
            Component.literal("Gave $count ")
                .append(Component.literal("colored bits in #$colorString to "))
                .append(player.displayName)
        }, true)
        return SINGLE_SUCCESS
    }

    private fun image2chisel(
        ctx: CommandContext<CommandSourceStack>,
        image: String,
        zMin: Int = 0,
        zMax: Int = 0,
        direction: Direction = ctx.source.entity?.let { Direction.orderedByNearest(it).first() } ?: Direction.SOUTH,
        paletteWildcard: String? = null,
        replaceAir: Boolean = true
    ): Int {
        if (zMin !in 0 until 16 || zMax !in 0 until 16) {
            ctx.source.sendSuccess({
                Component.literal("Z not in range").withStyle(ChatFormatting.RED)
            }, false)
            return -1
        }
        val zMax1 = if (zMax in 1 until zMin) maxOf(zMin + zMax - 1, zMin) else zMax
        try {
            val blockPos = BlockPos.containing(ctx.source.position)
            if (paletteWildcard != null) {
                val chiseledImage = ChiseledImage(image, paletteWildcard.contains('+'))

                ctx.source.sendSuccess({ Component.literal("image2chisel: Loaded $image") }, true)

                if (!chiseledImage.canPlace(ctx.source.playerOrException, blockPos, direction)) {
                    throw IllegalArgumentException("You cannot modify blocks in the target area")
                }
                val palette = PaletteIdentifier.fromString(paletteWildcard)
                chiseledImage.place(ctx.source.level, blockPos, zMin..zMax1, direction, palette, replaceAir)
            } else {
                val chiseledImage = ChiseledImage24(image)

                ctx.source.sendSuccess({ Component.literal("image2chisel24: Loaded $image") }, true)

                if (!chiseledImage.canPlace(ctx.source.playerOrException, blockPos, direction)) {
                    throw IllegalArgumentException("You cannot modify blocks in the target area")
                }

                chiseledImage.place(ctx.source.level, blockPos, zMin..zMax1, direction, PaletteIdentifier(), replaceAir)
            }

            ctx.source.sendSuccess({
                Component.literal(
                    "image2chisel: Placed $image at (${blockPos.toShortString()})"
                )
            }, true)
        } catch (e: Exception) {
            ctx.source.sendSuccess({
                Component.literal(e.message ?: e.javaClass.simpleName).withStyle(ChatFormatting.RED)
            }, false)
            return -2
        }

        return SINGLE_SUCCESS
    }

    private fun bitShaderHelp(ctx: CommandContext<FabricClientCommandSource>): Int {
        ctx.source.sendFeedback(Component.empty().apply {
            append(Component.translatable("gui.bitsandchisels.shader_types").withStyle(ChatFormatting.BOLD))
            for (i in 0..16) {
                append(
                    Component.translatable(
                    "gui.bitsandchisels.shader_type.explain",
                    "%2d".format(i),
                    Component.translatable("gui.bitsandchisels.shader_type.$i").withStyle(ChatFormatting.GOLD)
                ).withStyle(ChatFormatting.RESET))
            }
        })

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
                            (ctx.source.player?.mainHandItem?.item as? BlockItem)?.block?.defaultBlockState()
                        )
                    }
                    .then(
                        argument("player", EntityArgument.player())
                            .then(
                                argument("block", BlockStateArgument.block(commandRegistryAccess))
                                    .executes { ctx ->
                                        return@executes giveBit(
                                            ctx,
                                            ctx.source.player,
                                            BlockStateArgument.getBlock(ctx, "block").state
                                        )
                                    }
                            )
                            .then(
                                argument("color", StringArgumentType.string())
                                    .executes { ctx ->
                                        return@executes giveColoredBlockBit(
                                            ctx,
                                            ctx.source.player,
                                            StringArgumentType.getString(ctx, "color")
                                        )
                                    }
                            )
                    )
                    .then(
                        argument("block", BlockStateArgument.block(commandRegistryAccess))
                            .executes { ctx ->
                                return@executes giveBit(
                                    ctx,
                                    ctx.source.player,
                                    BlockStateArgument.getBlock(ctx, "block").state
                                )
                            }
                    )
                    .then(
                        literal("#")
                            .then(
                                argument("color", StringArgumentType.string())
                                    .executes { ctx ->
                                        return@executes giveColoredBlockBit(
                                            ctx,
                                            colorString = StringArgumentType.getString(ctx, "color")
                                        )
                                    }
                            )
                    )
            )
            dispatcher.register(
                literal("image2chisel")
                    .requires(Permissions.require(ModPermissions.COMMAND_IMAGE2CHISEL, 2))
                    .then(
                        literal("help")
                            .executes { ctx ->
                                ctx.source.sendSuccess({
                                    Component.literal(
                                        "Usage: /image2chisel <file name, must end with .png> " +
                                                "[z_min] [z_max or thickness] [north, south, east, west, up, down] " +
                                                "[palette, default \"minecraft:*_concrete\"] " +
                                                "[set_air, false to not change bits that are just air]"
                                    )
                                }, false)
                                return@executes SINGLE_SUCCESS
                            }
                    )
                    .then(
                        argument("image", StringArgumentType.string())
                            .executes { ctx ->
                                return@executes image2chisel(
                                    ctx,
                                    StringArgumentType.getString(ctx, "image")
                                )
                            }
                            .then(
                                argument("z_min", IntegerArgumentType.integer(0, 15))
                                    .then(
                                        argument("z_max", IntegerArgumentType.integer(0, 15))
                                            .executes { ctx ->
                                                return@executes image2chisel(
                                                    ctx,
                                                    StringArgumentType.getString(ctx, "image"),
                                                    IntegerArgumentType.getInteger(ctx, "z_min"),
                                                    IntegerArgumentType.getInteger(ctx, "z_max")
                                                )
                                            }
                                            .then(
                                                argument("direction", DirectionArgumentType.direction())
                                                    .executes { ctx ->
                                                        return@executes image2chisel(
                                                            ctx,
                                                            StringArgumentType.getString(ctx, "image"),
                                                            IntegerArgumentType.getInteger(ctx, "z_min"),
                                                            IntegerArgumentType.getInteger(ctx, "z_max"),
                                                            DirectionArgumentType.getDirection(ctx, "direction")
                                                        )
                                                    }
                                                    .then(
                                                        argument("palette", StringArgumentType.string())
                                                            .executes { ctx ->
                                                                return@executes image2chisel(
                                                                    ctx,
                                                                    StringArgumentType.getString(ctx, "image"),
                                                                    IntegerArgumentType.getInteger(ctx, "z_min"),
                                                                    IntegerArgumentType.getInteger(ctx, "z_max"),
                                                                    DirectionArgumentType.getDirection(ctx, "direction"),
                                                                    StringArgumentType.getString(ctx, "palette")
                                                                )
                                                            }
                                                            .then(
                                                                argument("set_air", BoolArgumentType.bool())
                                                                    .executes { ctx ->
                                                                        return@executes image2chisel(
                                                                            ctx,
                                                                            StringArgumentType.getString(ctx, "image"),
                                                                            IntegerArgumentType.getInteger(ctx, "z_min"),
                                                                            IntegerArgumentType.getInteger(ctx, "z_max"),
                                                                            DirectionArgumentType.getDirection(ctx, "direction"),
                                                                            StringArgumentType.getString(ctx, "palette"),
                                                                            BoolArgumentType.getBool(ctx, "set_air")
                                                                        )
                                                                    }
                                                            )
                                                    )
                                            )
                                            .then(
                                                argument("palette", StringArgumentType.string())
                                                    .executes { ctx ->
                                                        return@executes image2chisel(
                                                            ctx,
                                                            StringArgumentType.getString(ctx, "image"),
                                                            IntegerArgumentType.getInteger(ctx, "z_min"),
                                                            IntegerArgumentType.getInteger(ctx, "z_max"),
                                                            paletteWildcard = StringArgumentType.getString(ctx, "palette").ifEmpty { null }
                                                        )
                                                    }
                                                    .then(
                                                        argument("set_air", BoolArgumentType.bool())
                                                            .executes { ctx ->
                                                                return@executes image2chisel(
                                                                    ctx,
                                                                    StringArgumentType.getString(ctx, "image"),
                                                                    IntegerArgumentType.getInteger(ctx, "z_min"),
                                                                    IntegerArgumentType.getInteger(ctx, "z_max"),
                                                                    paletteWildcard = StringArgumentType.getString(ctx, "palette").ifEmpty { null },
                                                                    replaceAir = BoolArgumentType.getBool(ctx, "set_air")
                                                                )
                                                            }
                                                    )
                                            )
                                    )
                            )
                    )
            )
        }
    }

    fun registerAllClient() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("bitshaders").executes(::bitShaderHelp)
            )
        }
    }
}