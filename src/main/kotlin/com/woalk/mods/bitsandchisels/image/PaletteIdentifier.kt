package com.woalk.mods.bitsandchisels.image

import com.woalk.mods.bitsandchisels.BitsAndChisels
import com.woalk.mods.bitsandchisels.BitsBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.resources.Identifier
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

    private val withSkinColors = blockWildcard.contains('+')

    init {
        if (!blockWildcard.contains('*') && !withSkinColors) {
            throw IllegalArgumentException("blockWildcard must contain * or +")
        }
    }

    val bitBlockState: BlockState = if (blockWildcard.contains("glass")) {
        BitsAndChisels.BITS_BLOCK.defaultBlockState().setValue(BitsBlock.SHADER_TYPE, 3)
    } else BitsAndChisels.BITS_BLOCK.defaultBlockState()

    fun toIdentifier(wildcardInsert: String) = Identifier.fromNamespaceAndPath(
        namespace,
        blockWildcard.replace(if (withSkinColors) "+" else "*", wildcardInsert)
    )
}
