package io.github.coolmineman.bitsandchisels.image

import net.minecraft.util.Identifier
import java.lang.IllegalArgumentException

data class PaletteIdentifier(
    val namespace: String = "minecraft",
    val blockWildcard: String = "*_concrete"
) {
    companion object {
        fun fromString(string: String): PaletteIdentifier {
            val split = string.split(':', limit = 2)
            return if (split.size < 2) {
                PaletteIdentifier(blockWildcard = string)
            } else {
                PaletteIdentifier(split[0], split[1])
            }
        }
    }

    init {
        if (!blockWildcard.contains('*')) {
            throw IllegalArgumentException("blockWildcard must contain *")
        }
    }

    fun toIdentifier(wildcardInsert: String) = Identifier(
        namespace,
        blockWildcard.replace("*", wildcardInsert)
    )
}
