package com.woalk.mods.bitsandchisels.compat

import com.sk89q.jnbt.ByteArrayTag
import com.sk89q.jnbt.CompoundTag
import com.sk89q.jnbt.IntTag
import com.sk89q.jnbt.ListTag
import com.sk89q.jnbt.Tag
import com.woalk.mods.bitsandchisels.blocks.ColoredBlockState
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.item.DyeableItem

typealias Block3D = Array<Array<Array<BlockState?>>>

object BitCompoundUtil {
    fun Block3D(): Block3D = Array(16) {
        Array(16) {
            Array(16) { null }
        }
    }

    fun read3DBitArray(tag: CompoundTag, out: Block3D): Boolean {
        val palette = ArrayList<BlockState>()
        val palletteTag = tag.getList("palette")
        for (stateTag in palletteTag) {
            if (stateTag is CompoundTag) {
                palette.add(toBlockState(stateTag))
            }
        }
        val bits = tag.getList("bits")
        val bitsBytes = tag.getByteArray("bits_v2")
        var index = 0
        if (bits.isNotEmpty()) {
            for (i in 0..15) {
                for (j in 0..15) {
                    for (k in 0..15) {
                        out[i][j][k] = palette[bits[index].value as Int]
                        index++
                    }
                }
            }
        } else if (bitsBytes.isNotEmpty()) {
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

    fun write3DBitArray(from: Block3D, targetTag: CompoundTag? = null): CompoundTag {
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
        val tag = HashMap<String, Tag>()
        tag["bits_v2"] = ByteArrayTag(bits)
        val paletteTag = ListTag(CompoundTag::class.java, palette.map { fromBlockState(it) })
        tag["palette"] = paletteTag
        return if (targetTag != null) {
            targetTag.createBuilder().apply {
                tag.forEach(this::put)
            }.build()
        } else CompoundTag(tag)
    }

    private fun toBlockState(tag: CompoundTag?): BlockState {
        return if (tag == null) {
            Blocks.AIR.defaultState
        } else if (tag.containsKey(DyeableItem.COLOR_KEY)) {
            return ColoredBlockState(tag.getInt(DyeableItem.COLOR_KEY))
        } else {
            CompoundHelper.toBlockState(tag)
        }
    }

    private fun fromBlockState(state: BlockState?): CompoundTag {
        return when (state) {
            null -> CompoundHelper.fromBlockState(Blocks.AIR.defaultState)
            is ColoredBlockState -> {
                CompoundHelper.fromBlockState(state).run {
                    val map = value.toMutableMap()
                    map[DyeableItem.COLOR_KEY] = IntTag(state.color)
                    setValue(map)
                }
            }
            else -> CompoundHelper.fromBlockState(state)
        }
    }
}