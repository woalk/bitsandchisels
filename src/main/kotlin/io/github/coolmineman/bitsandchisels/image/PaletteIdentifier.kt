package io.github.coolmineman.bitsandchisels.image

import io.github.coolmineman.bitsandchisels.BitsAndChisels
import io.github.coolmineman.bitsandchisels.BitsBlock
import net.minecraft.block.BlockState
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

    val bitBlockState: BlockState = if (blockWildcard.contains("glass")) {
        BitsAndChisels.BITS_BLOCK.defaultState.with(BitsBlock.SHADER_TYPE, 3)
    } else BitsAndChisels.BITS_BLOCK.defaultState

    fun toIdentifier(wildcardInsert: String) = Identifier(
        namespace,
        blockWildcard.replace("*", wildcardInsert)
    )
}
