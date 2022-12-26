package io.github.coolmineman.bitsandchisels

import io.github.coolmineman.bitsandchisels.registry.ModCommands

object BitsAndChiselsMod {
    fun onInitialize() {
        ModCommands.registerAll()
    }
}