package com.woalk.mods.bitsandchisels.compat

import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.StateHolder
import net.minecraft.world.level.block.state.properties.Property
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import org.enginehub.linbus.tree.LinCompoundTag
import org.enginehub.linbus.tree.LinStringTag
import org.enginehub.linbus.tree.LinTag
import org.enginehub.linbus.tree.LinTagType

object CompoundHelper {
    fun toBlockState(nbt: LinCompoundTag): BlockState {
        if (nbt.value().containsKey("Name")) {
            // compat for 26.2 and older: convert id key
            nbt.value()["id"] = nbt.value()["Name"]
        }
        if (!nbt.value().containsKey("id")) {
            return Blocks.AIR.defaultBlockState()
        }
        val block = BuiltInRegistries.BLOCK[Identifier.parse(nbt.getTag("id", LinTagType.stringTag()).value())].get().value()
        var blockState = block.defaultBlockState()
        if (nbt.value().containsKey("Properties")) {
            // compat for 26.2 and older: convert properties key
            nbt.value()["properties"] = nbt.value()["Properties"]
        }
        if (nbt.value().containsKey("properties")) {
            val nbtCompound = nbt.getTag("properties", LinTagType.compoundTag())
            val stateManager = block.stateDefinition
            for (string in nbtCompound.value().keys) {
                val property = stateManager.getProperty(string) ?: continue
                blockState = withProperty(blockState, property, string, nbtCompound, nbt)
            }
        }
        return blockState
    }

    private fun <S : StateHolder<*, S>, T : Comparable<T>> withProperty(
        state: S,
        property: Property<T>,
        key: String,
        properties: LinCompoundTag,
        root: LinCompoundTag
    ): S {
        val optional = property.getValue(properties.getTag(key, LinTagType.stringTag()).value())
        return if (optional.isPresent) {
            state!!.setValue(
                property,
                optional.get()
            )
        } else state
    }

    fun fromBlockState(state: BlockState): LinCompoundTag {
        val nbtCompound = HashMap<String, LinTag<out Any>>()
        nbtCompound["id"] = LinStringTag.of(BuiltInRegistries.BLOCK.getKey(state.block).toString())
        val immutableMap = state.values.map { Pair(it.property, it.value) }.toList().toMap()
        if (!immutableMap.isEmpty()) {
            val nbtCompound2 = HashMap<String, LinTag<out Any>>()
            for ((key, value) in immutableMap) {
                nbtCompound2[key.name] = LinStringTag.of(nameValue(key, value))
            }
            nbtCompound["properties"] = LinCompoundTag.of(nbtCompound2)
        }
        return LinCompoundTag.of(nbtCompound)
    }

    private fun <T : Comparable<T>> nameValue(property: Property<in T>, value: Comparable<T>): String {
        @Suppress("UNCHECKED_CAST")
        return property.getName(value as T)
    }
}