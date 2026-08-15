package com.woalk.mods.bitsandchisels.utils

import com.mojang.brigadier.context.CommandContext
import com.woalk.mods.bitsandchisels.BitsAndChisels
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry
import net.minecraft.commands.arguments.StringRepresentableArgument
import net.minecraft.commands.synchronization.SingletonArgumentInfo
import net.minecraft.commands.CommandSourceStack
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.StringRepresentable
import net.minecraft.core.Direction

class DirectionArgumentType private constructor() : StringRepresentableArgument<Direction>(
    StringRepresentable.fromEnum(Direction::values),
    Direction::values
) {
    companion object {
        @JvmStatic
        fun direction() = DirectionArgumentType()

        @JvmStatic
        fun getDirection(ctx: CommandContext<CommandSourceStack>, id: String) =
            ctx.getArgument(id, Direction::class.java)

        fun registerDirectionArgumentType() {
            ArgumentTypeRegistry.registerArgumentType(
                ResourceLocation(BitsAndChisels.MODID, "direction"),
                DirectionArgumentType::class.java, SingletonArgumentInfo.contextFree(Companion::direction)
            )
        }
    }
}
