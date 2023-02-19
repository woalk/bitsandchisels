package com.woalk.mods.bitsandchisels.compat

import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.EmptyClipboardException
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.event.extent.EditSessionEvent
import com.sk89q.worldedit.util.eventbus.Subscribe
import com.woalk.mods.roleplay.registry.IModRegister

object WorldEditCompat : IModRegister {
    override fun registerAll() {
        WorldEdit.getInstance().eventBus.register(this)
        WorldEditCommands.registerAll()
    }

    @Suppress("unused")
    @Subscribe
    fun onEditSessionEvent(event: EditSessionEvent) {
        if (event.stage == EditSession.Stage.BEFORE_CHANGE && event.actor != null) try {
            val sessionManager = WorldEdit.getInstance().sessionManager.get(event.actor)
            val transform = sessionManager.clipboard.transform
            if (!transform.isIdentity) {
                val isPasting = Thread.currentThread().stackTrace.find {
                    it.methodName == "cmd\$_paste"
                } != null
                if (isPasting) {
                    event.extent = ChiselExtent(event.extent, transform)
                }
            }
        } catch (_: EmptyClipboardException) {
        }
    }
}