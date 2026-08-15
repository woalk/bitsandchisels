package com.woalk.mods.bitsandchisels.compat

import com.sk89q.jnbt.CompoundTag
import com.sk89q.jnbt.StringTag
import com.sk89q.jnbt.Tag
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.StateHolder
import net.minecraft.world.level.block.state.properties.Property
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.Registry

/** @see [net.minecraft.nbt.NbtHelper] */
object CompoundHelper {
    fun toBlockState(nbt: CompoundTag): BlockState {
        if (!nbt.containsKey("Name")) {
            return Blocks.AIR.defaultBlockState()
        }
        val block = Registry.BLOCK[ResourceLocation(nbt.getString("Name"))]
        var blockState = block.defaultBlockState()
        if (nbt.containsKey("Properties")) {
            val nbtCompound = nbt.value["Properties"] as CompoundTag
            val stateManager = block.stateDefinition
            for (string in nbtCompound.value.keys) {
                val property = stateManager.getProperty(string) ?: continue
                blockState = withProperty(blockState, property, string, nbtCompound, nbt)
            }
        }
        return blockState
    }

    private fun <S : StateHolder<*, S>?, T : Comparable<T>?> withProperty(
        state: S,
        property: Property<T>,
        key: String,
        properties: CompoundTag,
        root: CompoundTag
    ): S {
        val optional = property.getValue(properties.getString(key))
        return if (optional.isPresent) {
            state!!.setValue(
                property,
                optional.get()
            )
        } else state
    }

    fun fromBlockState(state: BlockState): CompoundTag {
        val nbtCompound = HashMap<String, Tag>()
        nbtCompound["Name"] = StringTag(Registry.BLOCK.getKey(state.block).toString())
        val immutableMap = state.values
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
        return property.getName(value as T)
    }
}