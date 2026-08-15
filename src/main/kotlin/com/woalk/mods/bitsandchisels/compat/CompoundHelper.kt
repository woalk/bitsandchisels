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
import net.minecraft.core.registries.BuiltInRegistries
import org.enginehub.linbus.tree.LinCompoundTag
import org.enginehub.linbus.tree.LinStringTag
import org.enginehub.linbus.tree.LinTag
import org.enginehub.linbus.tree.LinTagType

/** @see [net.minecraft.nbt.NbtHelper] */
object CompoundHelper {
    fun toBlockState(nbt: LinCompoundTag): BlockState {
        if (!nbt.value().containsKey("Name")) {
            return Blocks.AIR.defaultBlockState()
        }
        val block = BuiltInRegistries.BLOCK[ResourceLocation(nbt.getTag("Name", LinTagType.stringTag()).value())]
        var blockState = block.defaultBlockState()
        if (nbt.value().containsKey("Properties")) {
            val nbtCompound = nbt.getTag("Properties", LinTagType.compoundTag())
            val stateManager = block.stateDefinition
            for (string in nbtCompound.value().keys) {
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
        nbtCompound["Name"] = LinStringTag.of(BuiltInRegistries.BLOCK.getKey(state.block).toString())
        val immutableMap = state.values
        if (!immutableMap.isEmpty()) {
            val nbtCompound2 = HashMap<String, LinTag<out Any>>()
            for ((key, value) in immutableMap) {
                nbtCompound2[key.name] = LinStringTag.of(nameValue(key, value))
            }
            nbtCompound["Properties"] = LinCompoundTag.of(nbtCompound2)
        }
        return LinCompoundTag.of(nbtCompound)
    }

    private fun <T : Comparable<T>?> nameValue(property: Property<in T>, value: Comparable<T>): String {
        @Suppress("UNCHECKED_CAST")
        return property.getName(value as T)
    }
}