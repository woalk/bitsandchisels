package io.github.coolmineman.bitsandchisels.registry

import com.woalk.mods.roleplay.registry.IModRegister
import io.github.coolmineman.bitsandchisels.BitsAndChisels
import me.lucko.fabric.api.permissions.v0.Permissions
import java.util.*

object ModPermissions {
    private const val BASE = BitsAndChisels.MODID

    const val USE_CHISEL = "${BASE}.use_chisel"
    const val USE_CHISEL_DIAMOND = "${USE_CHISEL}.diamond"
    const val USE_CHISEL_IRON = "${USE_CHISEL}.iron"
    const val USE_CHISEL_SMART = "${USE_CHISEL}.smart"
    const val USE_CHISEL_ENDER = "${USE_CHISEL}.ender"
    const val USE_BIT = "${BASE}.use_bit"
    const val USE_BLUEPRINT = "${BASE}.use_blueprint"
    const val USE_WRENCH = "${BASE}.use_wrench"
    const val COMMAND_BIT = "${BASE}.bit_command"
    const val COMMAND_IMAGE2CHISEL = "${BASE}.image2chisel_command"

    const val USE_DEBUG_STICK = "minecraft.use_debug_stick"
}