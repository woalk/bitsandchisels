package io.github.coolmineman.bitsandchisels.image

import net.minecraft.util.Identifier
import java.lang.IllegalArgumentException

data class PaletteIdentifier(
    val namespace: String = "minecraft",
    val blockWildcard: String = "*_concrete"
) {
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
