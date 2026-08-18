package com.woalk.mods.bitsandchisels.compat

import com.woalk.mods.bitsandchisels.blocks.ColoredBlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import org.enginehub.linbus.tree.*

typealias Block3D = Array<Array<Array<BlockState?>>>

object BitCompoundUtil {
    fun Block3D(): Block3D = Array(16) {
        Array(16) {
            Array(16) { null }
        }
    }

    fun read3DBitArray(tag: LinCompoundTag, out: Block3D): Boolean {
        val palette = ArrayList<BlockState>()
        val paletteTag = tag.getListTag("palette", LinTagType.compoundTag())
        for (stateTag in paletteTag.value()) {
            palette.add(toBlockState(stateTag))
        }
        val bitsBytes = tag.getTag("bits_v2", LinTagType.byteArrayTag()).value()
        var index = 0
        if (bitsBytes.isNotEmpty()) {
            for (i in 0..15) {
                for (j in 0..15) {
                    for (k in 0..15) {
                        out[i][j][k] =
                            palette[bitsBytes[index + 1].toInt() shl 8 or (bitsBytes[index].toInt() and 0xFF)]
                        index += 2
                    }
                }
            }
        } else {
            return false
        }
        return true
    }

    fun write3DBitArray(from: Block3D, targetTag: LinCompoundTag? = null): LinCompoundTag {
        val palette = ArrayList<BlockState>()
        val bits = ByteArray(8192)
        var arrayIndex = 0
        for (i in 0..15) {
            for (j in 0..15) {
                for (k in 0..15) {
                    val state = from[i][j][k] ?: continue
                    var index = palette.indexOf(state).toShort()
                    if (index.toInt() == -1) {
                        palette.add(state)
                        index = (palette.size - 1).toShort()
                    }
                    bits[arrayIndex] = (index.toInt() and 0xff).toByte()
                    bits[arrayIndex + 1] = (index.toInt() shr 8 and 0xff).toByte()
                    arrayIndex += 2
                }
            }
        }
        val tag = HashMap<String, LinTag<out Any>>()
        tag["bits_v2"] = LinByteArrayTag.of(*bits)
        val paletteTag = LinListTag.of(LinTagType.compoundTag(), palette.map { fromBlockState(it) })
        tag["palette"] = paletteTag
        return if (targetTag != null) {
            targetTag.toBuilder().apply {
                tag.forEach(this::put)
            }.build()
        } else LinCompoundTag.of(tag)
    }

    private fun toBlockState(tag: LinCompoundTag?): BlockState {
        return if (tag == null) {
            Blocks.AIR.defaultBlockState()
        } else if (tag.value().containsKey(ColoredBlockState.TAG_COLOR)) {
            ColoredBlockState(tag.getTag(ColoredBlockState.TAG_COLOR, LinTagType.intTag()).value())
        } else {
            CompoundHelper.toBlockState(tag)
        }
    }

    private fun fromBlockState(state: BlockState?): LinCompoundTag {
        return when (state) {
            null -> CompoundHelper.fromBlockState(Blocks.AIR.defaultBlockState())
            is ColoredBlockState -> {
                CompoundHelper.fromBlockState(state).run {
                    val map = value().toMutableMap()
                    map[ColoredBlockState.TAG_COLOR] = LinIntTag.of(state.color)
                    LinCompoundTag.of(map)
                }
            }
            else -> CompoundHelper.fromBlockState(state)
        }
    }
}