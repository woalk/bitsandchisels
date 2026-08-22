package com.woalk.mods.bitsandchisels.registry

import com.woalk.mods.bitsandchisels.BitsAndChisels
import net.minecraft.resources.Identifier
import net.minecraft.core.Registry
import java.util.*

abstract class ModRegister<T, V : Any>(private val targetRegistry: Registry<V>) : IModRegister {
    private val tmpRegistry: MutableList<Pair<String, T>> = LinkedList<Pair<String, T>>()

    protected open fun register(id: String, toRegister: T): T {
        tmpRegistry.add(Pair(id, toRegister))
        return toRegister
    }

    protected abstract fun onRegister(toRegister: T): V

    protected open fun getNamespace(id: String) = BitsAndChisels.MODID

    protected open fun registerNow(id: String, toRegister: T) {
        Registry.register(targetRegistry, Identifier.fromNamespaceAndPath(getNamespace(id), id), onRegister(toRegister))
    }

    override fun registerAll() {
        for ((id, toRegister) in tmpRegistry) {
            registerNow(id, toRegister)
        }
        tmpRegistry.clear()
    }
}