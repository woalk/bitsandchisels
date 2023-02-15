package io.github.coolmineman.bitsandchisels.compat

import com.sk89q.jnbt.CompoundTag
import com.sk89q.jnbt.StringTag
import com.sk89q.jnbt.Tag
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.state.State
import net.minecraft.state.property.Property
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

/** @see [net.minecraft.nbt.NbtHelper] */
object CompoundHelper {
    fun toBlockState(nbt: CompoundTag): BlockState {
        if (!nbt.containsKey("Name")) {
            return Blocks.AIR.defaultState
        }
        val block = Registry.BLOCK[Identifier(nbt.getString("Name"))]
        var blockState = block.defaultState
        if (nbt.containsKey("Properties")) {
            val nbtCompound = nbt.value["Properties"] as CompoundTag
            val stateManager = block.stateManager
            for (string in nbtCompound.value.keys) {
                val property = stateManager.getProperty(string) ?: continue
                blockState = withProperty(blockState, property, string, nbtCompound, nbt)
            }
        }
        return blockState
    }

    private fun <S : State<*, S>?, T : Comparable<T>?> withProperty(
        state: S,
        property: Property<T>,
        key: String,
        properties: CompoundTag,
        root: CompoundTag
    ): S {
        val optional = property.parse(properties.getString(key))
        return if (optional.isPresent) {
            state!!.with(
                property,
                optional.get()
            )
        } else state
    }

    fun fromBlockState(state: BlockState): CompoundTag {
        val nbtCompound = HashMap<String, Tag>()
        nbtCompound["Name"] = StringTag(Registry.BLOCK.getId(state.block).toString())
        val immutableMap = state.entries
        if (!immutableMap.isEmpty()) {
            val nbtCompound2 = HashMap<String, Tag>()
            for ((key, value) in immutableMap) {
                nbtCompound2[key.name] = StringTag(nameValue(key, value))
            }
            nbtCompound["Properties"] = CompoundTag(nbtCompound2)
        }
        return CompoundTag(nbtCompound)
    }

    private fun <T : Comparable<T>?> nameValue(property: Property<in T>, value: Comparable<T>): String {
        @Suppress("UNCHECKED_CAST")
        return property.name(value as T)
    }
}