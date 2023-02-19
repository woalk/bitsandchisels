package com.woalk.mods.bitsandchisels.utils

import com.mojang.brigadier.context.CommandContext
import com.woalk.mods.bitsandchisels.BitsAndChisels
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry
import net.minecraft.command.argument.EnumArgumentType
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Identifier
import net.minecraft.util.StringIdentifiable
import net.minecraft.util.math.Direction

class DirectionArgumentType private constructor() : EnumArgumentType<Direction>(
    StringIdentifiable.createCodec(Direction::values),
    Direction::values
) {
    companion object {
        @JvmStatic
        fun direction() = DirectionArgumentType()

        @JvmStatic
        fun getDirection(ctx: CommandContext<ServerCommandSource>, id: String) =
            ctx.getArgument(id, Direction::class.java)

        fun registerDirectionArgumentType() {
            ArgumentTypeRegistry.registerArgumentType(
                Identifier(BitsAndChisels.MODID, "direction"),
                DirectionArgumentType::class.java, ConstantArgumentSerializer.of(Companion::direction)
            )
        }
    }
}
